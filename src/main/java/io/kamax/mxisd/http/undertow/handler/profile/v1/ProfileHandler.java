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

package io.kamax.mxisd.http.undertow.handler.profile.v1;

import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.http.undertow.handler.HomeserverProxyHandler;
import io.kamax.mxisd.profile.ProfileManager;
import io.kamax.mxisd.proxy.Response;
import io.undertow.server.HttpServerExchange;
import org.apache.http.client.methods.HttpGet;

import java.net.URI;
import java.util.Optional;

public class ProfileHandler extends HomeserverProxyHandler {

    public static final String UserID = "userId";
    public static final String Path = "/_matrix/client/r0/profile/{" + UserID + "}";

    protected ProfileManager mgr;

    public ProfileHandler(ProfileManager mgr) {
        this.mgr = mgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        String userId = getQueryParameter(exchange, UserID);
        _MatrixID mxId = MatrixID.asAcceptable(userId);
        URI target = URI.create(exchange.getRequestURL());
        Optional<String> accessTokenOpt = findAccessToken(exchange);

        HttpGet reqOut = new HttpGet(target);
        accessTokenOpt.ifPresent(accessToken -> reqOut.addHeader(headerName, headerValuePrefix + accessToken));

        Response res = mgr.enhance(mxId, reqOut);
        respond(exchange, res);
    }

}
