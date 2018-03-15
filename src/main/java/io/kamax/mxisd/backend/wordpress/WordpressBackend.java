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
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WordpressBackend {

    private final Logger log = LoggerFactory.getLogger(WordpressBackend.class);

    private WordpressConfig cfg;
    private CloseableHttpClient client;

    @Autowired
    public WordpressBackend(WordpressConfig cfg, CloseableHttpClient client) {
        this.cfg = cfg;
        this.client = client;

        if (!cfg.isEnabled()) {
            return;
        }

        validateConfig();
    }

    private void validateConfig() {
        log.info("Validating JWT auth endpoint");
        try (CloseableHttpResponse res = client.execute(new HttpGet(cfg.getEndpoint().getBase() + "/wp-json/jwt-auth/api/v1"))) {
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
        } catch (InvalidJsonException e) {
            log.warn("JWT auth endpoint check failed: Invalid JSON response: {}", e.getMessage());
        } catch (IOException e) {
            log.warn("JWT auth endpoint check failed: Could not read API endpoint: {}", e.getMessage());
        }
    }

    public boolean isEnabled() {
        return cfg.isEnabled();
    }

}
