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
import com.google.gson.JsonPrimitive;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.UserID;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.config.ExecConfig;
import io.kamax.mxisd.exception.ConfigurationException;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.util.TriFunction;
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
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class ExecAuthStore extends ExecStore implements AuthenticatorProvider {

    private final transient Logger log = LoggerFactory.getLogger(ExecAuthStore.class);

    private Map<String, Supplier<String>> inputTemplates;
    private Map<String, BiConsumer<String, ExecAuthResult>> outputMapper;

    private TriFunction<String, _MatrixID, String, String> inputMapper;

    private ExecConfig.Auth cfg;

    @Autowired
    public ExecAuthStore(ExecConfig cfg) {
        this.cfg = Objects.requireNonNull(cfg.getAuth());

        inputTemplates = new HashMap<>();
        inputTemplates.put(JsonType, () -> {
            JsonObject json = new JsonObject();
            json.addProperty("localpart", cfg.getToken().getLocalpart());
            json.addProperty("domain", cfg.getToken().getDomain());
            json.addProperty("mxid", cfg.getToken().getMxid());
            json.addProperty("password", cfg.getToken().getPassword());
            return GsonUtil.get().toJson(json);
        });
        inputTemplates.put(MultilinesType, () -> cfg.getToken().getLocalpart() + System.lineSeparator() +
                cfg.getToken().getDomain() + System.lineSeparator() +
                cfg.getToken().getMxid() + System.lineSeparator() +
                cfg.getToken().getPassword() + System.lineSeparator()
        );

        inputMapper = (input, uId, password) -> input.replace(cfg.getToken().getLocalpart(), uId.getLocalPart())
                .replace(cfg.getToken().getDomain(), uId.getDomain())
                .replace(cfg.getToken().getMxid(), uId.getId())
                .replace(cfg.getToken().getPassword(), password);

        outputMapper = new HashMap<>();
        outputMapper.put(JsonType, (output, result) -> {
            JsonObject data = GsonUtil.getObj(GsonUtil.parseObj(output), "auth");
            GsonUtil.findPrimitive(data, "success")
                    .map(JsonPrimitive::getAsBoolean)
                    .ifPresent(result::setSuccess);
            GsonUtil.findObj(data, "profile")
                    .flatMap(p -> GsonUtil.findString(p, "display_name"))
                    .ifPresent(v -> result.getProfile().setDisplayName(v));
        });
        outputMapper.put(MultilinesType, (output, result) -> {
            String[] lines = output.split("\\R");
            if (lines.length > 2) {
                throw new InternalServerError("Exec auth command returned more than 2 lines (" + lines.length + ")");
            }

            result.setSuccess(Optional.ofNullable(StringUtils.isEmpty(lines[0]) ? null : lines[0])
                    .map(v -> StringUtils.equalsAnyIgnoreCase(v, "true", "1"))
                    .orElse(result.isSuccess()));

            if (lines.length == 2) {
                Optional.ofNullable(StringUtils.isEmpty(lines[1]) ? null : lines[1])
                        .ifPresent(v -> result.getProfile().setDisplayName(v));
            }
        });

        validateConfig();
    }

    private void validateConfig() {
        if (StringUtils.isNotEmpty(cfg.getInput().getType()) && !inputTemplates.containsKey(cfg.getInput().getType())) {
            throw new ConfigurationException("Exec Auth input type is not valid: " + cfg.getInput().getType());
        }

        if (StringUtils.isNotEmpty(cfg.getOutput().getType()) && !outputMapper.containsKey(cfg.getOutput().getType())) {
            throw new ConfigurationException("Exec Auth output type is not valid: " + cfg.getInput().getType());
        }
    }

    @Override
    public boolean isEnabled() {
        return cfg.isEnabled();
    }

    @Override
    public ExecAuthResult authenticate(_MatrixID uId, String password) {
        Objects.requireNonNull(uId);
        Objects.requireNonNull(password);

        log.info("Performing authentication for {}", uId.getId());

        ExecAuthResult result = new ExecAuthResult();
        result.setId(new UserID(UserIdType.Localpart, uId.getLocalPart()));

        ProcessExecutor psExec = new ProcessExecutor().readOutput(true);

        List<String> args = new ArrayList<>();
        args.add(cfg.getCommand());
        args.addAll(cfg.getArgs().stream().map(arg -> inputMapper.apply(arg, uId, password)).collect(Collectors.toList()));
        psExec.command(args);

        psExec.environment(new HashMap<>(cfg.getEnv()).entrySet().stream()
                .peek(e -> e.setValue(inputMapper.apply(e.getValue(), uId, password)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        if (StringUtils.isNotBlank(cfg.getInput().getType())) {
            String template = cfg.getInput().getTemplate().orElseGet(inputTemplates.get(cfg.getInput().getType()));
            String input = inputMapper.apply(template, uId, password);
            psExec.redirectInput(IOUtils.toInputStream(input, StandardCharsets.UTF_8));
        }

        try {
            log.info("Executing {}", cfg.getCommand());
            ProcessResult psResult = psExec.execute();
            result.setExitStatus(psResult.getExitValue());
            String output = psResult.outputUTF8();
            log.debug("Command output:{}{}", System.lineSeparator(), output);

            log.info("Exit status: {}", result.getExitStatus());
            if (cfg.getExit().getSuccess().contains(result.getExitStatus())) {
                result.setSuccess(true);
                if (result.isSuccess() && StringUtils.isNotEmpty(output)) {
                    outputMapper.get(cfg.getOutput().getType()).accept(output, result);
                } else {
                    if (StringUtils.isNotEmpty(output)) {
                        log.info("Exec auth failed with output:{}{}", System.lineSeparator(), output);
                    }
                }
            } else if (cfg.getExit().getFailure().contains(result.getExitStatus())) {
                log.debug("{} stdout:{}{}", cfg.getCommand(), System.lineSeparator(), output);
                result.setSuccess(false);
            } else {
                log.error("{} stdout:{}{}", cfg.getCommand(), System.lineSeparator(), output);
                throw new InternalServerError("Exec auth command returned with unexpected exit status");
            }

            return result;
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new InternalServerError(e);
        }
    }

}
