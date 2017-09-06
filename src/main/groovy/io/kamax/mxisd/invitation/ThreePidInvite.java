package io.kamax.mxisd.invitation;

import io.kamax.matrix._MatrixID;

public class ThreePidInvite implements IThreePidInvite {

    private _MatrixID sender;
    private String medium;
    private String address;
    private String roomId;

    public ThreePidInvite(_MatrixID sender, String medium, String address, String roomId) {
        this.sender = sender;
        this.medium = medium;
        this.address = address;
        this.roomId = roomId;
    }

    @Override
    public _MatrixID getSender() {
        return sender;
    }

    @Override
    public String getMedium() {
        return medium;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public String getRoomId() {
        return roomId;
    }

}
