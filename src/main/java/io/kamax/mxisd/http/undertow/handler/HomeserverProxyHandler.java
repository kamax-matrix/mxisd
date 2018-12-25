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

package io.kamax.mxisd.http.undertow.handler;

import io.kamax.mxisd.exception.AccessTokenNotFoundException;
import io.kamax.mxisd.util.OptionalUtil;
import io.undertow.server.HttpServerExchange;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.Optional;

public abstract class HomeserverProxyHandler extends BasicHttpHandler {

    protected final static String headerName = "Authorization";
    protected final static String headerValuePrefix = "Bearer ";
    private final static String parameterName = "access_token";

    Optional<String> findAccessTokenInHeaders(HttpServerExchange exchange) {
        return Optional.ofNullable(exchange.getRequestHeaders().getFirst(headerName))
                .filter(header -> StringUtils.startsWith(header, headerValuePrefix))
                .map(header -> header.substring(headerValuePrefix.length()));
    }

    Optional<String> findAccessTokenInQuery(HttpServerExchange exchange) {
        return Optional.ofNullable(exchange.getQueryParameters().getOrDefault(parameterName, new LinkedList<>()).peekFirst());
    }

    public Optional<String> findAccessToken(HttpServerExchange exchange) {
        return OptionalUtil.findFirst(() -> findAccessTokenInHeaders(exchange), () -> findAccessTokenInQuery(exchange));
    }

    public String getAccessToken(HttpServerExchange exchange) {
        return findAccessToken(exchange).orElseThrow(AccessTokenNotFoundException::new);
    }

}
