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

package io.kamax.mxisd.http.undertow.handler.directory.v1;

import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.directory.DirectoryManager;
import io.kamax.mxisd.http.io.UserDirectorySearchRequest;
import io.kamax.mxisd.http.io.UserDirectorySearchResult;
import io.kamax.mxisd.http.undertow.handler.HomeserverProxyHandler;
import io.undertow.server.HttpServerExchange;

import java.net.URI;

public class UserDirectorySearchHandler extends HomeserverProxyHandler {

    public static final String Path = "/_matrix/client/r0/user_directory/search";

    private DirectoryManager mgr;

    public UserDirectorySearchHandler(DirectoryManager mgr) {
        this.mgr = mgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        String accessToken = getAccessToken(exchange);
        UserDirectorySearchRequest searchQuery = parseJsonTo(exchange, UserDirectorySearchRequest.class);
        URI target = URI.create(exchange.getRequestURL());
        UserDirectorySearchResult result = mgr.search(target, accessToken, searchQuery.getSearchTerm());

        respondJson(exchange, GsonUtil.get().toJson(result));
    }

}
