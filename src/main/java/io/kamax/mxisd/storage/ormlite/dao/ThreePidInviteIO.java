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

package io.kamax.mxisd.storage.ormlite.dao;

import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

@DatabaseTable(tableName = "invite_3pid")
public class ThreePidInviteIO {

    @DatabaseField(id = true)
    private String id;

    @DatabaseField(canBeNull = false)
    private String token;

    @DatabaseField(canBeNull = false)
    private String sender;

    @DatabaseField(canBeNull = false)
    private String medium;

    @DatabaseField(canBeNull = false)
    private String address;

    @DatabaseField(canBeNull = false)
    private String roomId;

    @DatabaseField
    private String properties;

    public ThreePidInviteIO() {
        // Needed for ORMLite
    }

    public ThreePidInviteIO(IThreePidInviteReply data) {
        this.id = data.getId();
        this.token = data.getToken();
        this.sender = data.getInvite().getSender().getId();
        this.medium = data.getInvite().getMedium();
        this.address = data.getInvite().getAddress();
        this.roomId = data.getInvite().getRoomId();
        this.properties = GsonUtil.get().toJson(data.getInvite().getProperties());
    }

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public String getSender() {
        return sender;
    }

    public String getMedium() {
        return medium;
    }

    public String getAddress() {
        return address;
    }

    public String getRoomId() {
        return roomId;
    }

    public Map<String, String> getProperties() {
        if (StringUtils.isBlank(properties)) {
            return new HashMap<>();
        }

        return GsonUtil.get().fromJson(properties, new TypeToken<Map<String, String>>() {
        }.getType());
    }

}
