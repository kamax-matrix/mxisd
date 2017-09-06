package io.kamax.mxisd.invitation;

import io.kamax.matrix._MatrixID;

public interface IThreePidInvite {

    _MatrixID getSender();

    String getMedium();

    String getAddress();

    String getRoomId();

}
