/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
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
import java.util.Objects;

public class MatrixIdInvite implements IMatrixIdInvite {

    private String roomId;
    private _MatrixID sender;
    private _MatrixID invitee;
    private String medium;
    private String address;
    private Map<String, String> properties;

    public MatrixIdInvite(String roomId, _MatrixID sender, _MatrixID invitee, String medium, String address, Map<String, String> properties) {
        this.roomId = Objects.requireNonNull(roomId);
        this.sender = Objects.requireNonNull(sender);
        this.invitee = Objects.requireNonNull(invitee);
        this.medium = Objects.requireNonNull(medium);
        this.address = Objects.requireNonNull(address);
        this.properties = new HashMap<>(Objects.requireNonNull(properties));
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
    public _MatrixID getInvitee() {
        return invitee;
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
