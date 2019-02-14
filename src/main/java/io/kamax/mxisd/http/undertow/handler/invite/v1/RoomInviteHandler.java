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

package io.kamax.mxisd.http.undertow.handler.invite.v1;

import com.google.gson.JsonObject;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.dns.ClientDnsOverwrite;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.exception.NotAllowedException;
import io.kamax.mxisd.exception.RemoteHomeServerException;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.invitation.InvitationManager;
import io.undertow.server.HttpServerExchange;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

public class RoomInviteHandler extends BasicHttpHandler {

    public static final String Path = "/_matrix/client/r0/rooms/{roomId}/invite";

    private static final Logger log = LoggerFactory.getLogger(RoomInviteHandler.class);

    private final CloseableHttpClient client;
    private final ClientDnsOverwrite dns;
    private final InvitationManager invMgr;

    public RoomInviteHandler(CloseableHttpClient client, ClientDnsOverwrite dns, InvitationManager invMgr) {
        this.client = client;
        this.dns = dns;
        this.invMgr = invMgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        String accessToken = getAccessToken(exchange);

        String whoamiUri = dns.transform(URI.create(exchange.getRequestURL()).resolve(URI.create("/_matrix/client/r0/account/whoami"))).toString();
        log.info("Who Am I URL: {}", whoamiUri);
        HttpGet whoAmIReq = new HttpGet(whoamiUri);
        whoAmIReq.addHeader("Authorization", "Bearer " + accessToken);
        _MatrixID uId;
        try (CloseableHttpResponse whoAmIRes = client.execute(whoAmIReq)) {
            int sc = whoAmIRes.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(whoAmIRes.getEntity());

            if (sc != 200) {
                log.warn("Unable to get caller identity from Homeserver - Status code: {}", sc);
                log.debug("Body: {}", body);
                throw new RemoteHomeServerException(body);
            }

            JsonObject json = GsonUtil.parseObj(body);
            Optional<String> uIdRaw = GsonUtil.findString(json, "user_id");
            if (!uIdRaw.isPresent()) {
                throw new RemoteHomeServerException("No User ID provided when checking identity");
            }

            uId = MatrixID.asAcceptable(uIdRaw.get());
        } catch (IOException e) {
            InternalServerError ex = new InternalServerError(e);
            log.error("Ref {}: Unable to fetch caller identity from Homeserver", ex.getReference());
            throw ex;
        }

        log.info("Processing room invite from {}", uId.getId());
        JsonObject reqBody = parseJsonObject(exchange);
        if (!invMgr.canInvite(uId, reqBody)) {
            throw new NotAllowedException("Your account is not allowed to invite that address");
        }

        log.info("Invite was allowing, relaying to the Homeserver");
        proxyPost(exchange, reqBody, client, dns);
    }

}
