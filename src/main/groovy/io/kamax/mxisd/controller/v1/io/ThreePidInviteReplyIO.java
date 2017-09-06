package io.kamax.mxisd.controller.v1.io;

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
