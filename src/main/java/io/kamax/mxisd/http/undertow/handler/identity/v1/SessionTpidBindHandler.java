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
import io.kamax.mxisd.exception.BadRequestException;
import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.invitation.InvitationManager;
import io.kamax.mxisd.session.SessionMananger;
import io.undertow.server.HttpServerExchange;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionTpidBindHandler extends BasicHttpHandler {

    public static final String Path = IsAPIv1.Base + "/3pid/bind";

    private transient final Logger log = LoggerFactory.getLogger(SessionTpidBindHandler.class);

    private SessionMananger mgr;
    private InvitationManager invMgr;

    public SessionTpidBindHandler(SessionMananger mgr, InvitationManager invMgr) {
        this.mgr = mgr;
        this.invMgr = invMgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        String sid = getQueryParameter(exchange, "sid");
        String secret = getQueryParameter(exchange, "client_secret");
        String mxid = getQueryParameter(exchange, "mxid");

        try {
            mgr.bind(sid, secret, mxid);
            respond(exchange, new JsonObject());
        } catch (BadRequestException e) {
            log.info("requested session was not validated");

            JsonObject obj = new JsonObject();
            obj.addProperty("errcode", "M_SESSION_NOT_VALIDATED");
            obj.addProperty("error", e.getMessage());
            respond(exchange, HttpStatus.SC_BAD_REQUEST, obj);
        } finally {
            // If a user registers, there is no standard login event. Instead, this is the only way to trigger
            // resolution at an appropriate time. Meh at synapse/Riot!
            invMgr.lookupMappingsForInvites();
        }
    }

}
