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
import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.session.SessionManager;
import io.undertow.server.HttpServerExchange;

public class SessionTpidUnbindHandler extends BasicHttpHandler {

    public static final String Path = IsAPIv1.Base + "/3pid/unbind";

    private final SessionManager sessionMgr;

    public SessionTpidUnbindHandler(SessionManager sessionMgr) {
        this.sessionMgr = sessionMgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        JsonObject body = parseJsonObject(exchange);
        sessionMgr.unbind(body);
        writeBodyAsUtf8(exchange, "{}");
    }

}
