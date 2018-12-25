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

package io.kamax.mxisd.http.undertow.handler.auth.v1;

import io.kamax.mxisd.auth.AuthManager;
import io.undertow.server.HttpServerExchange;
import org.apache.commons.io.IOUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public class LoginPostHandler extends LoginHandler {

    private AuthManager mgr;

    public LoginPostHandler(AuthManager mgr) {
        this.mgr = mgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        respondJson(exchange, mgr.proxyLogin(
                URI.create(exchange.getRequestURL()),
                IOUtils.toString(exchange.getInputStream(), StandardCharsets.UTF_8)
                )
        );
    }

}
