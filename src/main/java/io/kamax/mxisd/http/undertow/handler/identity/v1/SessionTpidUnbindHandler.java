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
import io.kamax.mxisd.exception.BadRequestException;
import io.kamax.mxisd.exception.NotAllowedException;
import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.session.SessionManager;
import io.undertow.server.HttpServerExchange;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionTpidUnbindHandler extends BasicHttpHandler {

    public static final String Path = IsAPIv1.Base + "/3pid/unbind";

    private static final Logger log = LoggerFactory.getLogger(SessionTpidUnbindHandler.class);

    private final SessionManager sessionMgr;

    public SessionTpidUnbindHandler(SessionManager sessionMgr) {
        this.sessionMgr = sessionMgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (StringUtils.isNotEmpty(auth)) {
            // We have a auth header to process
            if (StringUtils.startsWith(auth, "X-Matrix ")) {
                log.warn("A remote host attempted to unbind without proper authorization. Request was denied");
                log.warn("See https://github.com/kamax-matrix/mxisd/wiki/mxisd-and-your-privacy for more info");
                throw new NotAllowedException("3PID can only be removed via 3PID sessions, not via Homeserver signature");
            } else {
                throw new BadRequestException("Illegal authorization type");
            }
        }

        JsonObject body = parseJsonObject(exchange);
        sessionMgr.unbind(body);
        writeBodyAsUtf8(exchange, "{}");
    }

}
