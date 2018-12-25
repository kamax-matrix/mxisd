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

import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.auth.provider.BackendAuthResult;
import io.kamax.mxisd.config.rest.RestBackendConfig;
import io.kamax.mxisd.util.RestClientUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;

public class RestAuthProvider extends RestProvider implements AuthenticatorProvider {

    public RestAuthProvider(RestBackendConfig cfg) {
        super(cfg);
    }

    @Override
    public boolean isEnabled() {
        return cfg.isEnabled();
    }

    @Override
    public BackendAuthResult authenticate(_MatrixID mxid, String password) {
        RestAuthRequestJson auth = new RestAuthRequestJson();
        auth.setMxid(mxid.getId());
        auth.setLocalpart(mxid.getLocalPart());
        auth.setDomain(mxid.getDomain());
        auth.setPassword(password);

        HttpUriRequest req = RestClientUtils.post(cfg.getEndpoints().getAuth(), gson, "auth", auth);
        try (CloseableHttpResponse res = client.execute(req)) {
            int status = res.getStatusLine().getStatusCode();
            if (status < 200 || status >= 300) {
                return BackendAuthResult.failure();
            }

            return parser.parse(res, "auth", BackendAuthResult.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
