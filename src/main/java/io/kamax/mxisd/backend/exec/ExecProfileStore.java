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

import io.kamax.matrix._MatrixID;
import io.kamax.matrix._ThreePid;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.ExecConfig;
import io.kamax.mxisd.profile.JsonProfileRequest;
import io.kamax.mxisd.profile.JsonProfileResult;
import io.kamax.mxisd.profile.ProfileProvider;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ExecProfileStore extends ExecStore implements ProfileProvider {

    private ExecConfig.Profile cfg;

    public ExecProfileStore(ExecConfig cfg) {
        super(cfg);
        this.cfg = cfg.getProfile();
    }

    private Optional<JsonProfileResult> getFull(_MatrixID userId, ExecConfig.Process cfg) {
        Processor<Optional<JsonProfileResult>> p = new Processor<>(cfg);

        p.addJsonInputTemplate(tokens -> new JsonProfileRequest(tokens.getLocalpart(), tokens.getDomain(), tokens.getMxid()));
        p.addInputTemplate(PlainType, tokens -> tokens.getLocalpart() + System.lineSeparator()
                + tokens.getDomain() + System.lineSeparator()
                + tokens.getMxid() + System.lineSeparator()
        );

        p.addTokenMapper(cfg.getToken().getLocalpart(), userId::getLocalPart);
        p.addTokenMapper(cfg.getToken().getDomain(), userId::getDomain);
        p.addTokenMapper(cfg.getToken().getMxid(), userId::getId);

        p.withFailureDefault(v -> Optional.empty());

        p.addSuccessMapper(JsonType, output -> {
            if (StringUtils.isBlank(output)) {
                return Optional.empty();
            }

            return GsonUtil.findObj(GsonUtil.parseObj(output), "profile")
                    .map(obj -> GsonUtil.get().fromJson(obj, JsonProfileResult.class));
        });

        return p.execute();
    }

    @Override
    public Optional<String> getDisplayName(_MatrixID userId) {
        return getFull(userId, cfg.getDisplayName()).map(JsonProfileResult::getDisplayName);
    }

    @Override
    public List<_ThreePid> getThreepids(_MatrixID userId) {
        return getFull(userId, cfg.getThreePid())
                .map(p -> Collections.<_ThreePid>unmodifiableList(p.getThreepids()))
                .orElseGet(Collections::emptyList);
    }

    @Override
    public List<String> getRoles(_MatrixID userId) {
        return getFull(userId, cfg.getRole())
                .map(JsonProfileResult::getRoles)
                .orElseGet(Collections::emptyList);
    }

}
