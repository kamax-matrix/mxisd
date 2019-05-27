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

package io.kamax.mxisd.http.undertow.handler.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.auth.AuthManager;
import io.kamax.mxisd.auth.UserAuthResult;
import io.kamax.mxisd.exception.JsonMemberNotFoundException;
import io.kamax.mxisd.http.io.CredentialsValidationResponse;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestAuthHandler extends BasicHttpHandler {

    public static final String Path = "/_matrix-internal/identity/v1/check_credentials";

    private static final Logger log = LoggerFactory.getLogger(RestAuthHandler.class);

    private AuthManager mgr;

    public RestAuthHandler(AuthManager mgr) {
        this.mgr = mgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        JsonObject authData = parseJsonObject(exchange, "user");
        if (!authData.has("id") || !authData.has("password")) {
            throw new JsonMemberNotFoundException("Missing id or password keys");
        }

        String id = GsonUtil.getStringOrThrow(authData, "id");
        log.info("Requested to check credentials for {}", id);
        String password = GsonUtil.getStringOrThrow(authData, "password");

        UserAuthResult result = mgr.authenticate(id, password);
        CredentialsValidationResponse response = new CredentialsValidationResponse(result.isSuccess());

        if (result.isSuccess()) {
            response.setDisplayName(result.getDisplayName());
            response.getProfile().setThreePids(result.getThreePids());
        }

        JsonElement authObj = GsonUtil.get().toJsonTree(response);
        respond(exchange, GsonUtil.makeObj("auth", authObj));
    }

}
