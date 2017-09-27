/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
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

package io.kamax.mxisd.controller.identity.v1;

public class IdentityAPIv1 {

    public static final String BASE = "/_matrix/identity/api/v1";

    public static String getValidate(String medium, String sid, String secret, String token) {
        // FIXME use some kind of URLBuilder
        return BASE + "/validate/" + medium + "/submitToken?sid=" + sid + "&client_secret=" + secret + "&token=" + token;
    }

}
