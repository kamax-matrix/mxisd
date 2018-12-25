/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sàrl
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

package io.kamax.mxisd.backend.wordpress;

import com.google.gson.JsonObject;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.matrix.json.InvalidJsonException;
import io.kamax.mxisd.config.wordpress.WordpressConfig;
import io.kamax.mxisd.util.RestClientUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WordpressRestBackend {

    private transient final Logger log = LoggerFactory.getLogger(WordpressRestBackend.class);
    private final String jsonPath = "/wp-json";
    private final String jwtPath = "/jwt-auth/v1";

    private WordpressConfig cfg;
    private CloseableHttpClient client;

    private String jsonEndpoint;
    private String jwtEndpoint;

    private String token;

    public WordpressRestBackend(WordpressConfig cfg, CloseableHttpClient client) {
        this.cfg = cfg;
        this.client = client;

        if (!cfg.isEnabled()) {
            return;
        }

        jsonEndpoint = cfg.getRest().getBase() + jsonPath;
        jwtEndpoint = jsonEndpoint + jwtPath;
        validateConfig();
    }

    private void validateConfig() {
        log.info("Validating JWT auth endpoint");
        try (CloseableHttpResponse res = client.execute(new HttpGet(jwtEndpoint))) {
            int status = res.getStatusLine().getStatusCode();
            if (status != 200) {
                log.warn("JWT auth endpoint check failed: Got status code {}", status);
                return;
            }

            String data = EntityUtils.toString(res.getEntity());
            if (StringUtils.isBlank(data)) {
                log.warn("JWT auth endpoint check failed: Got no/empty body data");
            }

            JsonObject body = GsonUtil.parseObj(data);
            if (!body.has("namespace")) {
                log.warn("JWT auth endpoint check failed: invalid namespace");
            }

            log.info("JWT auth endpoint check succeeded");
        } catch (InvalidJsonException e) {
            log.warn("JWT auth endpoint check failed: Invalid JSON response: {}", e.getMessage());
        } catch (IOException e) {
            log.warn("JWT auth endpoint check failed: Could not read API endpoint: {}", e.getMessage());
        }
    }

    public boolean isEnabled() {
        return cfg.isEnabled();
    }

    protected WordpressAuthData authenticate(String username, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);
        HttpPost req = RestClientUtils.post(jwtEndpoint + "/token", body);
        try (CloseableHttpResponse res = client.execute(req)) {
            int status = res.getStatusLine().getStatusCode();
            String bodyRes = EntityUtils.toString(res.getEntity());
            if (status != 200) {
                throw new IllegalArgumentException(bodyRes);
            }

            return GsonUtil.get().fromJson(bodyRes, WordpressAuthData.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void authenticate() {
        WordpressAuthData data = authenticate(
                cfg.getRest().getCredential().getUsername(),
                cfg.getRest().getCredential().getPassword());
        log.info("Internal authentication: success, logged in as " + data.getUserNicename());
        token = data.getToken();
    }

    protected CloseableHttpResponse runRequest(HttpRequestBase request) throws IOException {
        request.setHeader("Authorization", "Bearer " + token);
        return client.execute(request);
    }

    public CloseableHttpResponse withAuthentication(HttpRequestBase request) throws IOException {
        CloseableHttpResponse response = runRequest(request);
        if (response.getStatusLine().getStatusCode() == 403) { //FIXME we should check the JWT expiration time
            authenticate();
            response = runRequest(request);
        }

        return response;
    }

}
