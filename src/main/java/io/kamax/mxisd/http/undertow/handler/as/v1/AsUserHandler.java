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

package io.kamax.mxisd.http.undertow.handler.as.v1;

import io.kamax.mxisd.as.AppSvcManager;
import io.undertow.server.HttpServerExchange;

import java.util.LinkedList;

public class AsUserHandler extends ApplicationServiceHandler {

    public static final String ID = "userId";
    public static final String Path = "/_matrix/app/v1/users/{" + ID + "}";

    private final AppSvcManager app;

    public AsUserHandler(AppSvcManager app) {
        this.app = app;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        String userId = exchange.getQueryParameters().getOrDefault(ID, new LinkedList<>()).peekFirst();
        app.withToken(getToken(exchange)).processUser(userId);
        respondJson(exchange, "{}");
    }

}
