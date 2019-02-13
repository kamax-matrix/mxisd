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
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.directory.DirectoryProvider;
import io.kamax.mxisd.http.io.UserDirectorySearchRequest;
import io.kamax.mxisd.http.io.UserDirectorySearchResult;
import org.apache.commons.lang3.StringUtils;

public class ExecDirectoryStore extends ExecStore implements DirectoryProvider {

    private ExecConfig.Directory cfg;
    private MatrixConfig mxCfg;

    public ExecDirectoryStore(MxisdConfig cfg) {
        this(cfg.getExec(), cfg.getMatrix());
    }

    public ExecDirectoryStore(ExecConfig cfg, MatrixConfig mxCfg) {
        super(cfg);
        this.cfg = cfg.getDirectory();
        this.mxCfg = mxCfg;
    }

    private UserDirectorySearchResult search(ExecConfig.Process cfg, UserDirectorySearchRequest request) {
        if (StringUtils.isEmpty(cfg.getCommand())) {
            return UserDirectorySearchResult.empty();
        }

        Processor<UserDirectorySearchResult> p = new Processor<>(cfg);

        p.addJsonInputTemplate(tokens -> new UserDirectorySearchRequest(tokens.getType(), tokens.getQuery()));
        p.addInputTemplate(PlainType, tokens -> tokens.getType() + System.lineSeparator() + tokens.getQuery());

        p.addTokenMapper(cfg.getToken().getType(), request::getBy);
        p.addTokenMapper(cfg.getToken().getQuery(), request::getSearchTerm);

        p.addSuccessMapper(JsonType, output -> {
            if (StringUtils.isBlank(output)) {
                return UserDirectorySearchResult.empty();
            }

            UserDirectorySearchResult response = GsonUtil.get().fromJson(output, UserDirectorySearchResult.class);
            for (UserDirectorySearchResult.Result result : response.getResults()) {
                result.setUserId(MatrixID.asAcceptable(result.getUserId(), mxCfg.getDomain()).getId());
            }
            return response;
        });
        p.withFailureDefault(output -> new UserDirectorySearchResult());

        return p.execute();
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
