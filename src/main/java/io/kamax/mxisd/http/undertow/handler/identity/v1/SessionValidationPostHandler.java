/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2019 Kamax Sarl
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

package io.kamax.mxisd.http.undertow.handler.identity.v1;

import com.google.gson.JsonObject;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.http.io.identity.SuccessStatusJson;
import io.kamax.mxisd.session.SessionManager;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormParserFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SessionValidationPostHandler extends SessionValidateHandler {

    private transient final Logger log = LoggerFactory.getLogger(SessionValidationPostHandler.class);

    private FormParserFactory factory;

    public SessionValidationPostHandler(SessionManager mgr) {
        super(mgr);
        factory = FormParserFactory.builder().build();
    }


    @Override
    public void handleRequest(HttpServerExchange exchange) throws IOException {
        log.info("Handling POST request to validate session");

        String sid;
        String secret;
        String token;

        String contentType = getContentType(exchange).orElseThrow(() -> new IllegalArgumentException("Content type header is not set"));
        if (StringUtils.equals(contentType, "application/json")) { // FIXME use MIME parsing tools
            log.info("Parsing as JSON data");

            JsonObject body = parseJsonObject(exchange);
            sid = GsonUtil.getStringOrThrow(body, "sid");
            secret = GsonUtil.getStringOrThrow(body, "client_secret");
            token = GsonUtil.getStringOrThrow(body, "token");
        } else if (StringUtils.equals(contentType, "application/x-www-form-urlencoded")) { // FIXME use MIME parsing tools
            log.info("Parsing as Form data");

            FormData data = factory.createParser(exchange).parseBlocking();
            sid = getOrThrow(data, "sid");
            secret = getOrThrow(data, "client_secret");
            token = getOrThrow(data, "token");
        } else {
            log.info("Unsupported Content type: {}", contentType);
            throw new IllegalArgumentException("Unsupported Content type: " + contentType);
        }

        handleRequest(sid, secret, token);
        respondJson(exchange, new SuccessStatusJson(true));
    }

}
