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

package io.kamax.mxisd.http.undertow.handler.status;

import com.google.gson.JsonObject;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.Mxisd;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.undertow.server.HttpServerExchange;

public class VersionHandler extends BasicHttpHandler {

    public static final String Path = "/version";

    private final String body;

    public VersionHandler() {
        JsonObject server = new JsonObject();
        server.addProperty("name", "mxisd");
        server.addProperty("version", Mxisd.Version);

        body = GsonUtil.getPrettyForLog(GsonUtil.makeObj("server", server));
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        respondJson(exchange, body);
    }

}
