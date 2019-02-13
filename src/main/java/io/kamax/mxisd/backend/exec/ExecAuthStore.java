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
import io.kamax.mxisd.backend.rest.RestAuthRequestJson;
import io.kamax.mxisd.config.ExecConfig;
import io.kamax.mxisd.exception.InternalServerError;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class ExecAuthStore extends ExecStore implements AuthenticatorProvider {

    private transient final Logger log = LoggerFactory.getLogger(ExecAuthStore.class);

    private ExecConfig.Auth cfg;

    public ExecAuthStore(ExecConfig cfg) {
        super(cfg);
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

        Processor<ExecAuthResult> p = new Processor<>(cfg);

        p.addTokenMapper(cfg.getToken().getLocalpart(), uId::getLocalPart);
        p.addTokenMapper(cfg.getToken().getDomain(), uId::getDomain);
        p.addTokenMapper(cfg.getToken().getMxid(), uId::getId);
        p.addTokenMapper(cfg.getToken().getPassword(), () -> password);

        p.addJsonInputTemplate(tokens -> {
            RestAuthRequestJson json = new RestAuthRequestJson();
            json.setLocalpart(tokens.getLocalpart());
            json.setDomain(tokens.getDomain());
            json.setMxid(tokens.getMxid());
            json.setPassword(tokens.getPassword());
            return json;
        });
        p.addInputTemplate(PlainType, tokens -> tokens.getLocalpart() + System.lineSeparator() +
                tokens.getDomain() + System.lineSeparator() +
                tokens.getMxid() + System.lineSeparator() +
                tokens.getPassword() + System.lineSeparator()
        );

        p.withExitHandler(pr -> result.setExitStatus(pr.getExitValue()));

        p.withSuccessHandler(pr -> result.setSuccess(true));
        p.withSuccessDefault(o -> result);
        p.addSuccessMapper(JsonType, output -> {
            JsonObject data = GsonUtil.getObj(GsonUtil.parseObj(output), "auth");
            GsonUtil.findPrimitive(data, "success")
                    .map(JsonPrimitive::getAsBoolean)
                    .ifPresent(result::setSuccess);
            GsonUtil.findObj(data, "profile")
                    .flatMap(profile -> GsonUtil.findString(profile, "display_name"))
                    .ifPresent(v -> result.getProfile().setDisplayName(v));

            return result;
        });
        p.addSuccessMapper(PlainType, output -> {
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

            return result;
        });

        p.withFailureHandler(pr -> result.setSuccess(false));
        p.withFailureDefault(o -> result);

        return p.execute();
    }

}
