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

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.exception.BadRequestException;
import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.io.identity.StoreInviteRequest;
import io.kamax.mxisd.http.io.identity.ThreePidInviteReplyIO;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.invitation.IThreePidInvite;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import io.kamax.mxisd.invitation.InvitationManager;
import io.kamax.mxisd.invitation.ThreePidInvite;
import io.kamax.mxisd.storage.crypto.KeyManager;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.QueryParameterUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Map;

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
        String reqContentType = getContentType(exchange).orElse("application/octet-stream");
        JsonObject invJson = new JsonObject();

        if (StringUtils.startsWith(reqContentType, "application/json")) {
            invJson = parseJsonObject(exchange);
        }

        // Backward compatibility for pre-r0.1.0 implementations
        else if (StringUtils.startsWith(reqContentType, "application/x-www-form-urlencoded")) {
            String body = getBodyUtf8(exchange);
            Map<String, Deque<String>> parms = QueryParameterUtils.parseQueryString(body, StandardCharsets.UTF_8.name());
            for (Map.Entry<String, Deque<String>> entry : parms.entrySet()) {
                if (entry.getValue().size() == 0) {
                    return;
                }

                if (entry.getValue().size() > 1) {
                    throw new BadRequestException("key " + entry.getKey() + " has more than one value");
                }

                invJson.addProperty(entry.getKey(), entry.getValue().peekFirst());
            }
        } else {
            throw new BadRequestException("Unsupported Content-Type: " + reqContentType);
        }

        Type parmType = new TypeToken<Map<String, String>>() {
        }.getType();
        Map<String, String> parameters = GsonUtil.get().fromJson(invJson, parmType);
        StoreInviteRequest inv = GsonUtil.get().fromJson(invJson, StoreInviteRequest.class);
        _MatrixID sender = MatrixID.asAcceptable(inv.getSender());

        IThreePidInvite invite = new ThreePidInvite(sender, inv.getMedium(), inv.getAddress(), inv.getRoomId(), parameters);
        IThreePidInviteReply reply = invMgr.storeInvite(invite);

        // FIXME the key info must be set by the invitation manager in the reply object!
        respondJson(exchange, new ThreePidInviteReplyIO(reply, keyMgr.getPublicKeyBase64(keyMgr.getServerSigningKey().getId()), cfg.getPublicUrl()));
    }

}
