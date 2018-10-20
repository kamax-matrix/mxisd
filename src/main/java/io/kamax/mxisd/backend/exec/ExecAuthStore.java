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
import io.kamax.mxisd.exception.InternalServerError;
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
import java.util.stream.Collectors;

@Component
public class ExecAuthStore extends ExecStore implements AuthenticatorProvider {

    private final transient Logger log = LoggerFactory.getLogger(ExecAuthStore.class);

    private ExecConfig.Auth cfg;

    @Autowired
    public ExecAuthStore(ExecConfig cfg) {
        this.cfg = Objects.requireNonNull(cfg.getAuth());
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
        args.addAll(cfg.getArgs().stream().map(arg -> arg
                .replace(cfg.getToken().getLocalpart(), uId.getLocalPart())
                .replace(cfg.getToken().getDomain(), uId.getDomain())
                .replace(cfg.getToken().getMxid(), uId.getId())
                .replace(cfg.getToken().getPassword(), password)
        ).collect(Collectors.toList()));
        psExec.command(args);

        psExec.environment(new HashMap<>(cfg.getEnv()).entrySet().stream().peek(e -> {
            e.setValue(e.getValue().replace(cfg.getToken().getLocalpart(), uId.getLocalPart()));
            e.setValue(e.getValue().replace(cfg.getToken().getDomain(), uId.getDomain()));
            e.setValue(e.getValue().replace(cfg.getToken().getMxid(), uId.getId()));
            e.setValue(e.getValue().replace(cfg.getToken().getPassword(), password));
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        if (StringUtils.isNotBlank(cfg.getInput())) {
            if (StringUtils.equals("json", cfg.getInput())) {
                JsonObject input = new JsonObject();
                input.addProperty("localpart", uId.getLocalPart());
                input.addProperty("mxid", uId.getId());
                input.addProperty("password", password);
                psExec.redirectInput(IOUtils.toInputStream(GsonUtil.get().toJson(input), StandardCharsets.UTF_8));
            } else {
                throw new InternalServerError(cfg.getInput() + " is not a valid executable input format");
            }
        }

        try {
            log.info("Executing {}", cfg.getCommand());
            ProcessResult psResult = psExec.execute();
            result.setExitStatus(psResult.getExitValue());
            String output = psResult.outputUTF8();

            log.info("Exit status: {}", result.getExitStatus());
            if (cfg.getExit().getSuccess().contains(result.getExitStatus())) {
                result.setSuccess(true);
                if (result.isSuccess()) {
                    if (StringUtils.equals("json", cfg.getOutput())) {
                        JsonObject data = GsonUtil.parseObj(output);
                        GsonUtil.findPrimitive(data, "success")
                                .map(JsonPrimitive::getAsBoolean)
                                .ifPresent(result::setSuccess);
                        GsonUtil.findObj(data, "profile")
                                .flatMap(p -> GsonUtil.findString(p, "display_name"))
                                .ifPresent(v -> result.getProfile().setDisplayName(v));
                    } else {
                        log.debug("Command output:{}{}", "\n", output);
                    }
                }
            } else if (cfg.getExit().getFailure().contains(result.getExitStatus())) {
                log.debug("{} stdout:{}{}", cfg.getCommand(), "\n", output);
                result.setSuccess(false);
            } else {
                log.error("{} stdout:{}{}", cfg.getCommand(), "\n", output);
                throw new InternalServerError("Exec auth command returned with unexpected exit status");
            }

            return result;
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new InternalServerError(e);
        }
    }

}
