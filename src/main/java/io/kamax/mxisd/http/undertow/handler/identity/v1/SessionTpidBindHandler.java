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
import io.kamax.mxisd.http.io.identity.BindRequest;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.invitation.InvitationManager;
import io.kamax.mxisd.session.SessionManager;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.QueryParameterUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Map;

public class SessionTpidBindHandler extends BasicHttpHandler {

    public static final String Path = IsAPIv1.Base + "/3pid/bind";

    private transient final Logger log = LoggerFactory.getLogger(SessionTpidBindHandler.class);

    private SessionManager mgr;
    private InvitationManager invMgr;

    public SessionTpidBindHandler(SessionManager mgr, InvitationManager invMgr) {
        this.mgr = mgr;
        this.invMgr = invMgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        BindRequest bindReq = new BindRequest();
        bindReq.setSid(getQueryParameter(exchange, BindRequest.Keys.SessionID));
        bindReq.setSecret(getQueryParameter(exchange, BindRequest.Keys.Secret));
        bindReq.setUserId(getQueryParameter(exchange, BindRequest.Keys.UserID));

        String reqContentType = getContentType(exchange).orElse("application/octet-stream");
        if (StringUtils.equals("application/x-www-form-urlencoded", reqContentType)) {
            String body = getBodyUtf8(exchange);
            Map<String, Deque<String>> parms = QueryParameterUtils.parseQueryString(body, StandardCharsets.UTF_8.name());
            bindReq.setSid(getQueryParameter(parms, BindRequest.Keys.SessionID));
            bindReq.setSecret(getQueryParameter(parms, BindRequest.Keys.Secret));
            bindReq.setUserId(getQueryParameter(parms, BindRequest.Keys.UserID));
        } else if (StringUtils.equals("application/json", reqContentType)) {
            bindReq = parseJsonTo(exchange, BindRequest.class);
        } else {
            log.warn("Unknown encoding in 3PID session bind: {}", reqContentType);
            log.warn("The request will most likely fail");
        }

        try {
            mgr.bind(bindReq.getSid(), bindReq.getSecret(), bindReq.getUserId());
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
