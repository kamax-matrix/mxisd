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
import io.kamax.mxisd.exception.FeatureNotAvailable;
import io.kamax.mxisd.exception.NotAllowedException;
import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionTpidUnbindHandler extends BasicHttpHandler {

    public static final String Path = IsAPIv1.Base + "/3pid/unbind";

    private transient final Logger log = LoggerFactory.getLogger(SessionTpidUnbindHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        JsonObject body = parseJsonObject(exchange);

        // TODO also check for HS header to know which domain attempting the unbind
        if (body.entrySet().size() == 2 && body.has("mxisd") && body.has("threepid")) {
            /* This is a HS request to remove a 3PID and is considered:
             * - An attack on user privacy
             * - A baffling spec breakage requiring IS and HS 3PID info to be independent [1]
             * - A baffling spec breakage that 3PID (un)bind is only one way [2]
             *
             * Given the lack of response on our extensive feedback on the proposal [3] which has not landed in the spec yet [4],
             * We'll be denying such unbind requests and will inform users using their 3PID that a fraudulent attempt of
             * removing their 3PID binding has been attempting but blocked.
             *
             * [1]: https://matrix.org/docs/spec/client_server/r0.4.0.html#adding-account-administrative-contact-information
             * [2]: https://matrix.org/docs/spec/identity_service/r0.1.0.html#privacy
             * [3]: https://docs.google.com/document/d/135g2muVxmuml0iUnLoTZxk8M2ZSt3kJzg81chGh51yg/edit
             * [4]: https://github.com/matrix-org/matrix-doc/issues/1194
             */

            log.warn("A remote host attempted to unbind without proper authorization. Request was denied");

            // TODO notify the 3PID owner

            throw new NotAllowedException("You have attempted to alter 3PID bindings, which can only be done by the 3PID owner directly. " +
                    "We have informed the 3PID owner of your fraudulent attempt.");
        }

        throw new FeatureNotAvailable("Unbind using a 3PID session is not defined in the spec");
    }

}
