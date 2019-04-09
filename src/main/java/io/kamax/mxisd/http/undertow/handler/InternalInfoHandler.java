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

package io.kamax.mxisd.http.undertow.handler;

import io.undertow.server.HttpServerExchange;

import java.util.concurrent.ThreadLocalRandom;

public class InternalInfoHandler extends BasicHttpHandler {

    /*
     * This endpoint should never be called as being entierly custom as per instructions of New Vector,
     * the author of that endpoint.
     *
     * Used for the first time at https://github.com/matrix-org/synapse/pull/4681/files#diff-a73c645c44a17da6ab70f256da6b60afR41
     *
     * Full context: https://matrix.to/#/!YkZelGRiqijtzXZODa:matrix.org/$15510967621328WMKVu:kamax.io?via=matrix.org
     * Room name: #matrix-spec
     * Room alias: #matrix-spec:matrix.org
     */
    public static final String Path = "/_matrix/identity/api/{version}/internal-info";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // We will return a random status code in all possible error codes
        int type = ThreadLocalRandom.current().nextInt(4, 6) * 100; // Random 4 or 5, times 100
        int status = type + ThreadLocalRandom.current().nextInt(0, 100); // Random 0 to 99

        respond(exchange, status, "M_FORBIDDEN", "This endpoint is under quarantine and possibly wrongfully labeled stable.");
    }

}
