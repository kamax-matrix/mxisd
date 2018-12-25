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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.net.URI;

public class LoginGetHandler extends LoginHandler {

    private AuthManager mgr;
    private CloseableHttpClient client;

    public LoginGetHandler(AuthManager mgr, CloseableHttpClient client) {
        this.mgr = mgr;
        this.client = client;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        URI target = URI.create(exchange.getRequestURL());

        try (CloseableHttpResponse hsResponse = client.execute(new HttpGet(mgr.resolveProxyUrl(target)))) {
            // TODO deal with headers, content-type, encoding, etc.
            respondJson(exchange, hsResponse.getStatusLine().getStatusCode(), EntityUtils.toString(hsResponse.getEntity()));
        }
    }

}
