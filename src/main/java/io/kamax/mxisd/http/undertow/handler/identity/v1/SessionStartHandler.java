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
import io.kamax.matrix.ThreePid;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.io.identity.RequestTokenResponse;
import io.kamax.mxisd.http.io.identity.SessionEmailTokenRequestJson;
import io.kamax.mxisd.http.io.identity.SessionPhoneTokenRequestJson;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.session.SessionMananger;
import io.undertow.server.HttpServerExchange;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionStartHandler extends BasicHttpHandler {

    public static final String Medium = "medium";
    public static final String Path = IsAPIv1.Base + "/validate/{" + Medium + "}/requestToken";

    private transient final Logger log = LoggerFactory.getLogger(SessionStartHandler.class);

    private SessionMananger mgr;

    public SessionStartHandler(SessionMananger mgr) {
        this.mgr = mgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        String medium = getPathVariable(exchange, "medium");

        if (ThreePidMedium.Email.is(medium)) {
            SessionEmailTokenRequestJson req = parseJsonTo(exchange, SessionEmailTokenRequestJson.class);
            ThreePid threepid = new ThreePid(req.getMedium(), req.getValue());

            respondJson(exchange, new RequestTokenResponse(mgr.create(
                    getRemoteHostAddress(exchange),
                    threepid,
                    req.getSecret(),
                    req.getAttempt(),
                    req.getNextLink())));
        } else if (ThreePidMedium.PhoneNumber.is(medium)) {
            SessionPhoneTokenRequestJson req = parseJsonTo(exchange, SessionPhoneTokenRequestJson.class);
            ThreePid threepid = new ThreePid(req.getMedium(), req.getValue());

            String sessionId = mgr.create(
                    getRemoteHostAddress(exchange),
                    threepid,
                    req.getSecret(),
                    req.getAttempt(),
                    req.getNextLink());

            JsonObject res = new JsonObject();
            res.addProperty("sid", sessionId);
            res.addProperty(threepid.getMedium(), threepid.getAddress());
            respond(exchange, res);
        } else {
            JsonObject obj = new JsonObject();
            obj.addProperty("errcode", "M_INVALID_3PID_TYPE");
            obj.addProperty("error", medium + " is not supported as a 3PID type");
            respond(exchange, HttpStatus.SC_BAD_REQUEST, obj);
        }
    }

}
