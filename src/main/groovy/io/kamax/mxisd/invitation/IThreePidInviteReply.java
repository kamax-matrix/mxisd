package io.kamax.mxisd.invitation;

public interface IThreePidInviteReply {

    IThreePidInvite getInvite();

    String getToken();

    String getDisplayName();

}
