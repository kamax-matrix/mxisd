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

package io.kamax.mxisd.http.undertow.handler.identity.v1;

import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.lookup.ALookupRequest;
import io.undertow.server.HttpServerExchange;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Arrays;

public abstract class LookupHandler extends BasicHttpHandler {

    private transient final Logger log = LoggerFactory.getLogger(LookupHandler.class);

    protected void setRequesterInfo(ALookupRequest lookup, HttpServerExchange exchange) {
        InetSocketAddress addr = (InetSocketAddress) exchange.getConnection().getPeerAddress();
        lookup.setRequester(addr.getAddress().getHostAddress());
        String xff = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        log.debug("XFF header: {}", xff);
        lookup.setRecursive(StringUtils.isBlank(xff));
        if (!lookup.isRecursive()) {
            lookup.setRecurseHosts(Arrays.asList(xff.split(",")));
            lookup.setRequester(lookup.getRecurseHosts().get(lookup.getRecurseHosts().size() - 1));
        }

        lookup.setUserAgent(exchange.getRequestHeaders().getFirst("User-Agent"));
    }

}
