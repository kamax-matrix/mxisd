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

package io.kamax.mxisd.http.io.identity;

import io.kamax.mxisd.invitation.IThreePidInviteReply;

import java.util.Collections;
import java.util.List;

public class ThreePidInviteReplyIO {

    private String token;
    private List<Key> public_keys;
    private String display_name;

    public ThreePidInviteReplyIO(IThreePidInviteReply reply, String pubKey, String publicUrl) {
        this.token = reply.getToken();
        this.public_keys = Collections.singletonList(new Key(pubKey, publicUrl));
        this.display_name = reply.getDisplayName();
    }

    public String getToken() {
        return token;
    }

    public List<Key> getPublic_keys() {
        return public_keys;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public class Key {
        private String key_validity_url;
        private String public_key;

        public Key(String key, String publicUrl) {
            this.key_validity_url = publicUrl + "/_matrix/identity/api/v1/pubkey/isvalid";
            this.public_key = key;
        }

        public String getKey_validity_url() {
            return key_validity_url;
        }

        public String getPublic_key() {
            return public_key;
        }
    }

}
