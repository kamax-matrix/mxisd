package io.kamax.mxisd.controller.v1.io;

import io.kamax.mxisd.invitation.IThreePidInviteReply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThreePidInviteReplyIO {

    private String token;
    private List<Key> public_keys;
    private String display_name;

    public ThreePidInviteReplyIO(IThreePidInviteReply reply, String pubKey) {
        this.token = reply.getToken();
        this.public_keys = new ArrayList<>(Arrays.asList(new Key(pubKey)));
        this.display_name = reply.getDisplayName();
    }

    public class Key {
        private String key_validity_url;
        private String public_key;

        public Key(String key) {
            this.key_validity_url = "https://example.org/_matrix/fixme"; // FIXME have a proper URL even if synapse does not check
            this.public_key = key;
        }
    }

}
