package io.kamax.mxisd.invitation;

public class ThreePidInviteReply implements IThreePidInviteReply {

    private IThreePidInvite invite;
    private String token;
    private String displayName;

    public ThreePidInviteReply(IThreePidInvite invite, String token, String displayName) {
        this.invite = invite;
        this.token = token;
        this.displayName = displayName;
    }

    @Override
    public IThreePidInvite getInvite() {
        return invite;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

}
