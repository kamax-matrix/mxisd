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

package io.kamax.mxisd.invitation;

import io.kamax.matrix._MatrixID;

import java.util.HashMap;
import java.util.Map;

public class ThreePidInvite implements IThreePidInvite {

    private _MatrixID sender;
    private String medium;
    private String address;
    private String roomId;
    private Map<String, String> properties;

    public ThreePidInvite(_MatrixID sender, String medium, String address, String roomId) {
        this.sender = sender;
        this.medium = medium;
        this.address = address;
        this.roomId = roomId;
        this.properties = new HashMap<>();
    }

    public ThreePidInvite(_MatrixID sender, String medium, String address, String roomId, Map<String, String> properties) {
        this(sender, medium, address, roomId);
        this.properties = properties;
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

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

}
