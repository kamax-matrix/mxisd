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

import io.kamax.matrix.MatrixID;
import io.kamax.matrix.crypto.KeyManager;
import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.io.identity.ThreePidInviteReplyIO;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.invitation.IThreePidInvite;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import io.kamax.mxisd.invitation.InvitationManager;
import io.kamax.mxisd.invitation.ThreePidInvite;
import io.undertow.server.HttpServerExchange;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StoreInviteHandler extends BasicHttpHandler {

    public static final String Path = IsAPIv1.Base + "/store-invite";

    private ServerConfig cfg;
    private InvitationManager invMgr;
    private KeyManager keyMgr;

    public StoreInviteHandler(ServerConfig cfg, InvitationManager invMgr, KeyManager keyMgr) {
        this.cfg = cfg;
        this.invMgr = invMgr;
        this.keyMgr = keyMgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        Map<String, String> parameters = new HashMap<>();

        for (Map.Entry<String, Deque<String>> entry : exchange.getQueryParameters().entrySet()) {
            if (Objects.nonNull(entry.getValue().peekFirst())) {
                parameters.put(entry.getKey(), entry.getValue().peekFirst());
            }
        }

        // TODO test with missing parameters to see behaviour
        String sender = parameters.get("sender");
        String medium = parameters.get("medium");
        String address = parameters.get("address");
        String roomId = parameters.get("room_id");

        IThreePidInvite invite = new ThreePidInvite(MatrixID.asAcceptable(sender), medium, address, roomId, parameters);
        IThreePidInviteReply reply = invMgr.storeInvite(invite);

        respondJson(exchange, new ThreePidInviteReplyIO(reply, keyMgr.getPublicKeyBase64(keyMgr.getCurrentIndex()), cfg.getPublicUrl()));
    }

}
