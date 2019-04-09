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

package io.kamax.mxisd.http.undertow.handler.register.v1;

import com.google.gson.JsonObject;
import io.kamax.matrix.ThreePid;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.dns.ClientDnsOverwrite;
import io.kamax.mxisd.exception.NotAllowedException;
import io.kamax.mxisd.http.io.identity.SessionEmailTokenRequestJson;
import io.kamax.mxisd.http.io.identity.SessionPhoneTokenRequestJson;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.registration.RegistrationManager;
import io.undertow.server.HttpServerExchange;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Register3pidRequestTokenHandler extends BasicHttpHandler {

    public static final String Key = "medium";
    public static final String Path = "/_matrix/client/r0/register/{" + Key + "}/requestToken";

    private static final Logger log = LoggerFactory.getLogger(Register3pidRequestTokenHandler.class);

    private final RegistrationManager mgr;
    private final ClientDnsOverwrite dns;
    private final CloseableHttpClient client;

    public Register3pidRequestTokenHandler(RegistrationManager mgr, ClientDnsOverwrite dns, CloseableHttpClient client) {
        this.mgr = mgr;
        this.dns = dns; // FIXME this shouldn't be in here but in the manager
        this.client = client; // FIXME this shouldn't be in here but in the manager
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        JsonObject body = parseJsonObject(exchange);

        String medium = getPathVariable(exchange, Key);
        String address = GsonUtil.findString(body, "address").orElse("");
        if (ThreePidMedium.Email.is(medium)) {
            address = GsonUtil.get().fromJson(body, SessionEmailTokenRequestJson.class).getValue();
        } else if (ThreePidMedium.PhoneNumber.is(medium)) {
            address = GsonUtil.get().fromJson(body, SessionPhoneTokenRequestJson.class).getValue();
        } else {
            log.warn("Unsupported 3PID medium. We attempted to extract the address but the call might fail");
        }

        ThreePid tpid = new ThreePid(medium, address);
        if (!mgr.isAllowed(tpid)) {
            throw new NotAllowedException("Your " + medium + " address cannot be used for registration");
        }

        proxyPost(exchange, body, client, dns);
    }

}
