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

public class StoreInviteRequest {

    // Available keys from Spec + HS implementations reverse-engineering
    //
    // Synapse: https://github.com/matrix-org/synapse/blob/a219ce87263ad9be887cf039a04b4a1f06b7b0b8/synapse/handlers/room_member.py#L826
    public static class Keys {

        public static final String Medium = "medium";
        public static final String Address = "address";
        public static final String RoomID = "room_id";
        public static final String RoomAlias = "room_alias"; // Not in the spec, arbitrary
        public static final String RoomAvatarURL = "room_avatar_url"; // Not in the spec, arbitrary
        public static final String RoomJoinRules = "room_join_rules"; // Not in the spec, arbitrary
        public static final String RoomName = "room_name"; // Not in the spec, arbitrary
        public static final String Sender = "sender";
        public static final String SenderDisplayName = "sender_display_name"; // Not in the spec, arbitrary
        public static final String SenderAvatarURL = "sender_avatar_url"; // Not in the spec, arbitrary
        public static final String GuestAccessToken = "guest_access_token"; // Not in the spec, arbitrary
        public static final String GuestUserID = "guest_user_id"; // Not in the spec, arbitrary

    }

    @SerializedName(Keys.Medium)
    private String medium;

    @SerializedName(Keys.Address)
    private String address;

    @SerializedName(Keys.RoomID)
    private String roomId;

    @SerializedName(Keys.RoomAlias)
    private String roomAlias;

    @SerializedName(Keys.RoomAvatarURL)
    private String roomAvatarUrl;

    @SerializedName(Keys.RoomJoinRules)
    private String roomJoinRules;

    @SerializedName(Keys.RoomName)
    private String roomName;

    @SerializedName(Keys.Sender)
    private String sender;

    @SerializedName(Keys.SenderDisplayName)
    private String senderDisplayName;

    @SerializedName(Keys.SenderAvatarURL)
    private String senderAvatarUrl;

    @SerializedName(Keys.GuestAccessToken)
    private String guestAccessToken;

    @SerializedName(Keys.GuestUserID)
    private String guestUserId;

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomAlias() {
        return roomAlias;
    }

    public void setRoomAlias(String roomAlias) {
        this.roomAlias = roomAlias;
    }

    public String getRoomAvatarUrl() {
        return roomAvatarUrl;
    }

    public void setRoomAvatarUrl(String roomAvatarUrl) {
        this.roomAvatarUrl = roomAvatarUrl;
    }

    public String getRoomJoinRules() {
        return roomJoinRules;
    }

    public void setRoomJoinRules(String roomJoinRules) {
        this.roomJoinRules = roomJoinRules;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }

    public void setSenderAvatarUrl(String senderAvatarUrl) {
        this.senderAvatarUrl = senderAvatarUrl;
    }

    public String getGuestAccessToken() {
        return guestAccessToken;
    }

    public void setGuestAccessToken(String guestAccessToken) {
        this.guestAccessToken = guestAccessToken;
    }

    public String getGuestUserId() {
        return guestUserId;
    }

    public void setGuestUserId(String guestUserId) {
        this.guestUserId = guestUserId;
    }

}
