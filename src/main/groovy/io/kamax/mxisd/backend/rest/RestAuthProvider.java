/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.auth.UserAuthResult;
import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.config.rest.RestBackendConfig;
import io.kamax.mxisd.util.GsonParser;
import io.kamax.mxisd.util.RestClientUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthProvider implements AuthenticatorProvider {

    private RestBackendConfig cfg;
    private Gson gson;
    private GsonParser parser;
    private CloseableHttpClient client;

    @Autowired
    public RestAuthProvider(RestBackendConfig cfg) {
        this.cfg = cfg;

        client = HttpClients.createDefault();
        gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        parser = new GsonParser(gson);
    }

    @Override
    public boolean isEnabled() {
        return cfg.isEnabled();
    }

    @Override
    public UserAuthResult authenticate(String id, String password) {
        _MatrixID mxid = new MatrixID(id);
        RestAuthRequestJson auth = new RestAuthRequestJson();
        auth.setMxid(id);
        auth.setLocalpart(mxid.getLocalPart());
        auth.setDomain(mxid.getDomain());
        auth.setPassword(password);

        HttpUriRequest req = RestClientUtils.post(cfg.getEndpoints().getAuth(), gson, "auth", auth);
        try (CloseableHttpResponse res = client.execute(req)) {
            UserAuthResult result = new UserAuthResult();

            int status = res.getStatusLine().getStatusCode();
            if (status < 200 || status >= 300) {
                return result.failure();
            }

            RestAuthReplyJson reply = parser.parse(res, "auth", RestAuthReplyJson.class);
            if (!reply.isSuccess()) {
                return result.failure();
            }

            return result.success(reply.getMxid(), reply.getProfile().getDisplayName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
