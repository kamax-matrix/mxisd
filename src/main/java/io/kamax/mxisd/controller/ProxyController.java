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

package io.kamax.mxisd.controller;

import io.kamax.mxisd.exception.AccessTokenNotFoundException;
import io.kamax.mxisd.util.OptionalUtil;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class ProxyController {

    private final static String headerName = "Authorization";
    private final static String headerValuePrefix = "Bearer ";
    private final static String parameterName = "access_token";

    Optional<String> findAccessTokenInHeaders(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(headerName))
                .filter(header -> StringUtils.startsWith(header, headerValuePrefix))
                .map(header -> header.substring(headerValuePrefix.length()));
    }

    Optional<String> findAccessTokenInQuery(HttpServletRequest request) {
        return Optional.ofNullable(request.getParameter(parameterName));
    }

    public Optional<String> findAccessToken(HttpServletRequest request) {
        return OptionalUtil.findFirst(() -> findAccessTokenInHeaders(request), () -> findAccessTokenInQuery(request));
    }

    public String getAccessToken(HttpServletRequest request) {
        return findAccessToken(request).orElseThrow(AccessTokenNotFoundException::new);
    }

}
