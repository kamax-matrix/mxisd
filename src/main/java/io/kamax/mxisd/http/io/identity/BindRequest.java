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

package io.kamax.mxisd.http.io.identity;

import com.google.gson.annotations.SerializedName;

public class BindRequest {

    public static class Keys {

        public static final String SessionID = "sid";
        public static final String Secret = "client_secret";
        public static final String UserID = "mxid";
    }

    @SerializedName(Keys.SessionID)
    private String sid;

    @SerializedName(Keys.Secret)
    private String secret;

    @SerializedName(Keys.UserID)
    private String userId;

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}
