/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxisd.backend.exec;

import com.google.gson.JsonObject;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.ExecConfig;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.exception.ConfigurationException;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.exception.NotImplementedException;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.provider.IThreePidProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class ExecIdentityStore extends ExecStore implements IThreePidProvider {

    private final Logger log = LoggerFactory.getLogger(ExecIdentityStore.class);

    private final ExecConfig.Identity cfg;
    private final MatrixConfig mxCfg;

    private BiFunction<String, SingleLookupRequest, String> singleInputMap;
    private Map<String, Supplier<String>> singleInputTemplates;
    private Map<String, Function<String, Optional<_MatrixID>>> singleOutputMap;

    @Autowired
    public ExecIdentityStore(ExecConfig cfg, MatrixConfig mxCfg) {
        this.cfg = cfg.getIdentity();
        this.mxCfg = mxCfg;

        singleInputMap = (v, request) -> v.replace(cfg.getToken().getMedium(), request.getType())
                .replace(cfg.getToken().getAddress(), request.getThreePid());

        singleInputTemplates = new HashMap<>();
        singleInputTemplates.put(JsonType, () -> {
            JsonObject json = new JsonObject();
            json.addProperty("medium", cfg.getToken().getMedium());
            json.addProperty("address", cfg.getToken().getAddress());
            return GsonUtil.get().toJson(json);
        });
        singleInputTemplates.put(MultilinesType, () -> cfg.getToken().getMedium()
                + System.lineSeparator()
                + cfg.getToken().getAddress()
        );

        singleOutputMap = new HashMap<>();
        singleOutputMap.put(JsonType, output -> {
            if (StringUtils.isBlank(output)) {
                return Optional.empty();
            }

            return GsonUtil.findObj(GsonUtil.parseObj(output), "lookup").map(lookup -> {
                String type = GsonUtil.getStringOrThrow(lookup, "type");
                String value = GsonUtil.getStringOrThrow(lookup, "value");
                if (StringUtils.equals(type, "uid")) {
                    return MatrixID.asAcceptable(value, mxCfg.getDomain());
                }

                if (StringUtils.equals(type, "mxid")) {
                    return MatrixID.asAcceptable(value);
                }

                throw new InternalServerError("Invalid user type: " + type);
            });
        });
        singleOutputMap.put(MultilinesType, output -> {
            String[] lines = output.split("\\R");
            if (lines.length > 2) {
                throw new InternalServerError("Exec auth command returned more than 2 lines (" + lines.length + ")");
            }

            if (lines.length == 1 && StringUtils.isBlank(lines[0])) {
                return Optional.empty();
            }

            String type = StringUtils.trimToEmpty(lines.length == 1 ? "uid" : lines[0]);
            String value = StringUtils.trimToEmpty(lines.length == 2 ? lines[1] : lines[0]);

            if (StringUtils.equals(type, "uid")) {
                return Optional.of(MatrixID.asAcceptable(value, mxCfg.getDomain()));
            }

            if (StringUtils.equals(type, "mxid")) {
                return Optional.of(MatrixID.asAcceptable(value));
            }

            throw new InternalServerError("Invalid user type: " + type);
        });

        validateConfig();
    }

    private void validateConfig() {
        if (StringUtils.isNotEmpty(cfg.getInput().getType()) && !singleInputTemplates.containsKey(cfg.getInput().getType())) {
            throw new ConfigurationException("Exec Identity Single Lookup: input type is not valid: " + cfg.getInput().getType());
        }

        if (StringUtils.isNotEmpty(cfg.getOutput().getType()) && !singleOutputMap.containsKey(cfg.getOutput().getType())) {
            throw new ConfigurationException("Exec Auth output type is not valid: " + cfg.getInput().getType());
        }
    }

    @Override
    public boolean isEnabled() {
        return cfg.isEnabled();
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public int getPriority() {
        return cfg.getPriority();
    }

    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        ProcessExecutor psExec = new ProcessExecutor().readOutput(true);

        List<String> args = new ArrayList<>();
        args.add(cfg.getCommand());
        args.addAll(cfg.getArgs().stream().map(arg -> singleInputMap.apply(arg, request)).collect(Collectors.toList()));
        psExec.command(args);

        psExec.environment(new HashMap<>(cfg.getEnv()).entrySet().stream()
                .peek(e -> e.setValue(singleInputMap.apply(e.getValue(), request)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        if (StringUtils.isNotBlank(cfg.getInput().getType())) {
            String template = cfg.getInput().getTemplate().orElseGet(singleInputTemplates.get(cfg.getInput().getType()));
            String input = singleInputMap.apply(template, request);
            psExec.redirectInput(IOUtils.toInputStream(input, StandardCharsets.UTF_8));
        }

        try {
            log.info("Executing {}", cfg.getCommand());
            ProcessResult psResult = psExec.execute();
            String output = psResult.outputUTF8();
            log.debug("Command output:{}{}", System.lineSeparator(), output);

            log.info("Exit status: {}", psResult.getExitValue());
            if (cfg.getExit().getSuccess().contains(psResult.getExitValue())) {
                if (StringUtils.isBlank(output)) {
                    return Optional.empty();
                }

                return singleOutputMap.get(cfg.getOutput().getType())
                        .apply(output)
                        .map(mxId -> new SingleLookupReply(request, mxId));
            } else if (cfg.getExit().getFailure().contains(psResult.getExitValue())) {
                log.debug("{} stdout:{}{}", cfg.getCommand(), System.lineSeparator(), output);
                return Optional.empty();
            } else {
                log.error("{} stdout:{}{}", cfg.getCommand(), System.lineSeparator(), output);
                throw new InternalServerError("Exec auth command returned with unexpected exit status");
            }
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new InternalServerError(e);
        }
    }

    @Override
    public List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        throw new NotImplementedException(this.getClass().getName());
    }

}
