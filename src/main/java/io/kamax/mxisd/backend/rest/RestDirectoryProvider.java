/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Kamax Sarl
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

package io.kamax.mxisd.backend.rest;

import io.kamax.matrix.MatrixID;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.rest.RestBackendConfig;
import io.kamax.mxisd.directory.DirectoryProvider;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.http.io.UserDirectorySearchRequest;
import io.kamax.mxisd.http.io.UserDirectorySearchResult;
import io.kamax.mxisd.util.RestClientUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RestDirectoryProvider extends RestProvider implements DirectoryProvider {

    private MatrixConfig mxCfg;

    public RestDirectoryProvider(RestBackendConfig cfg, MatrixConfig mxCfg) {
        super(cfg);
        this.mxCfg = mxCfg;
    }

    private UserDirectorySearchResult search(String by, String query) {
        UserDirectorySearchRequest request = new UserDirectorySearchRequest(query);
        request.setBy(by);
        try (CloseableHttpResponse httpResponse = client.execute(RestClientUtils.post(cfg.getEndpoints().getDirectory(), request))) {
            int status = httpResponse.getStatusLine().getStatusCode();
            if (status < 200 || status >= 300) {
                throw new InternalServerError("REST backend: Error: " + IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8));
            }

            UserDirectorySearchResult response = parser.parse(httpResponse, UserDirectorySearchResult.class);
            for (UserDirectorySearchResult.Result result : response.getResults()) {
                result.setUserId(MatrixID.asAcceptable(result.getUserId(), mxCfg.getDomain()).getId());
            }

            return response;
        } catch (IOException e) {
            throw new InternalServerError("REST backend: I/O error: " + e.getMessage());
        }
    }

    @Override
    public UserDirectorySearchResult searchByDisplayName(String query) {
        return search("name", query);
    }

    @Override
    public UserDirectorySearchResult searchBy3pid(String query) {
        return search("threepid", query);
    }

}
