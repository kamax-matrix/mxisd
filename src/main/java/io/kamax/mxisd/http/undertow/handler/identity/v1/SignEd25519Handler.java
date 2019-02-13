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
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.matrix.json.MatrixJson;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import io.kamax.mxisd.invitation.InvitationManager;
import io.kamax.mxisd.storage.crypto.SignatureManager;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignEd25519Handler extends BasicHttpHandler {

    public static final String Path = IsAPIv1.Base + "/sign-ed25519";

    private static final Logger log = LoggerFactory.getLogger(SignEd25519Handler.class);

    private final MxisdConfig cfg;
    private final InvitationManager invMgr;
    private final SignatureManager signMgr;

    public SignEd25519Handler(MxisdConfig cfg, InvitationManager invMgr, SignatureManager signMgr) {
        this.cfg = cfg;
        this.invMgr = invMgr;
        this.signMgr = signMgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        JsonObject body = parseJsonObject(exchange);

        _MatrixID mxid = MatrixID.asAcceptable(GsonUtil.getStringOrThrow(body, "mxid"));
        String token = GsonUtil.getStringOrThrow(body, "token");
        String privKey = GsonUtil.getStringOrThrow(body, "private_key");

        IThreePidInviteReply reply = invMgr.getInvite(token, privKey);
        _MatrixID sender = reply.getInvite().getSender();

        JsonObject res = new JsonObject();
        res.addProperty("token", token);
        res.addProperty("sender", sender.getId());
        res.addProperty("mxid", mxid.getId());
        res.add("signatures", signMgr.signMessageGson(cfg.getServer().getName(), MatrixJson.encodeCanonical(res)));

        log.info("Signed data for invite using token {}", token);
        respondJson(exchange, res);
    }

}
