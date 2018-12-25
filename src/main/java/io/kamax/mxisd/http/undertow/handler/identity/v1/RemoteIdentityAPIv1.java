/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Kamax Sarl
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

public class RemoteIdentityAPIv1 {

    public static final String BASE = "/_matrix/identity/remote/api/v1";
    public static final String SESSION_REQUEST_TOKEN = BASE + "/validate/requestToken";
    public static final String SESSION_CHECK = BASE + "/validate/check";

    public static String getRequestToken(String id, String secret) {
        return SESSION_REQUEST_TOKEN + "?sid=" + id + "&client_secret=" + secret;
    }

    public static String getSessionCheck(String id, String secret) {
        return SESSION_CHECK + "?sid=" + id + "&client_secret=" + secret;
    }

}
