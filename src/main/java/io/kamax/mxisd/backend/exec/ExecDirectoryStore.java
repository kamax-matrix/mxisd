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

import io.kamax.matrix.MatrixID;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.ExecConfig;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.controller.directory.v1.io.UserDirectorySearchRequest;
import io.kamax.mxisd.controller.directory.v1.io.UserDirectorySearchResult;
import io.kamax.mxisd.directory.IDirectoryProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExecDirectoryStore extends ExecStore implements IDirectoryProvider {

    private ExecConfig.Directory cfg;
    private MatrixConfig mxCfg;

    @Autowired
    public ExecDirectoryStore(ExecConfig cfg, MatrixConfig mxCfg) {
        this.cfg = cfg.getDirectory();
        this.mxCfg = mxCfg;
    }

    @Override
    public boolean isEnabled() {
        return cfg.isEnabled();
    }

    private UserDirectorySearchResult search(ExecConfig.Process cfg, UserDirectorySearchRequest request) {
        Processor<UserDirectorySearchResult> processor = new Processor<>();
        processor.withConfig(cfg);
        processor.addInputTemplate(JsonType, tokens -> GsonUtil.get().toJson(new UserDirectorySearchRequest(tokens.getType(), tokens.getQuery())));
        processor.addInputTemplate(MultilinesType, tokens -> tokens.getType() + System.lineSeparator() + tokens.getQuery());

        processor.addTokenMapper(cfg.getToken().getType(), request::getBy);
        processor.addTokenMapper(cfg.getToken().getQuery(), request::getSearchTerm);

        processor.addSuccessMapper(JsonType, output -> {
            UserDirectorySearchResult response = GsonUtil.get().fromJson(output, UserDirectorySearchResult.class);
            for (UserDirectorySearchResult.Result result : response.getResults()) {
                result.setUserId(MatrixID.asAcceptable(result.getUserId(), mxCfg.getDomain()).getId());
            }
            return response;
        });
        processor.withFailureDefault(output -> new UserDirectorySearchResult());

        return processor.execute();
    }

    @Override
    public UserDirectorySearchResult searchByDisplayName(String query) {
        return search(cfg.getSearch().getByName(), new UserDirectorySearchRequest("name", query));
    }

    @Override
    public UserDirectorySearchResult searchBy3pid(String query) {
        return search(cfg.getSearch().getByName(), new UserDirectorySearchRequest("threepid", query));
    }

}
