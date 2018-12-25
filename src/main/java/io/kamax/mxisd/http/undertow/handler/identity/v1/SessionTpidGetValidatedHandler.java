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
import io.kamax.mxisd.exception.SessionNotValidatedException;
import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.lookup.ThreePidValidation;
import io.kamax.mxisd.session.SessionMananger;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionTpidGetValidatedHandler extends BasicHttpHandler {

    public static final String Path = IsAPIv1.Base + "/3pid/getValidated3pid";

    private transient final Logger log = LoggerFactory.getLogger(SessionTpidGetValidatedHandler.class);

    private SessionMananger mgr;

    public SessionTpidGetValidatedHandler(SessionMananger mgr) {
        this.mgr = mgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        String sid = getQueryParameter(exchange, "sid");
        String secret = getQueryParameter(exchange, "client_secret");

        try {
            ThreePidValidation pid = mgr.getValidated(sid, secret);

            JsonObject obj = new JsonObject();
            obj.addProperty("medium", pid.getMedium());
            obj.addProperty("address", pid.getAddress());
            obj.addProperty("validated_at", pid.getValidation().toEpochMilli());

            respond(exchange, obj);
        } catch (SessionNotValidatedException e) {
            log.info("Session {} was requested but has not yet been validated", sid);
            throw e;
        }
    }

}
