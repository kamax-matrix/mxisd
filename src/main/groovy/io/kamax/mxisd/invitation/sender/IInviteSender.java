package io.kamax.mxisd.invitation.sender;

import io.kamax.mxisd.invitation.IThreePidInviteReply;

public interface IInviteSender {

    String getMedium();

    void send(IThreePidInviteReply invite);

}
