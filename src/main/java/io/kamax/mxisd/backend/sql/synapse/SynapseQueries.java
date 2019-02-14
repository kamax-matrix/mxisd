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

package io.kamax.mxisd.backend.sql.synapse;

import io.kamax.mxisd.exception.ConfigurationException;
import org.apache.commons.lang.StringUtils;

public class SynapseQueries {

    public static String getUserId(String type, String domain) {
        if (StringUtils.equals("sqlite", type)) {
            return "'@' || p.user_id || ':" + domain + "'";
        } else if (StringUtils.equals("postgresql", type)) {
            return "concat('@',p.user_id,':" + domain + "')";
        } else {
            throw new ConfigurationException("Invalid Synapse SQL type: " + type);
        }
    }

    public static String getDisplayName() {
        return "SELECT displayname FROM profiles WHERE user_id = ?";
    }

    public static String getThreepids() {
        return "SELECT medium, address FROM user_threepids WHERE user_id = ?";
    }

    public static String getRoles() {
        return "SELECT DISTINCT(group_id) FROM group_users WHERE user_id = ?";
    }

    public static String findByDisplayName(String type, String domain) {
        if (StringUtils.equals("sqlite", type)) {
            return "select " + getUserId(type, domain) + ", displayname from profiles p where displayname like ?";
        } else if (StringUtils.equals("postgresql", type)) {
            return "select " + getUserId(type, domain) + ", displayname from profiles p where displayname ilike ?";
        } else {
            throw new ConfigurationException("Invalid Synapse SQL type: " + type);
        }
    }

    public static String findByThreePidAddress(String type, String domain) {
        if (StringUtils.equals("sqlite", type)) {
            return "select t.user_id, p.displayname " +
                    "from user_threepids t JOIN profiles p on t.user_id = " + getUserId(type, domain) + " " +
                    "where t.address like ?";
        } else if (StringUtils.equals("postgresql", type)) {
            return "select t.user_id, p.displayname " +
                    "from user_threepids t JOIN profiles p on t.user_id = " + getUserId(type, domain) + " " +
                    "where t.address ilike ?";
        } else {
            throw new ConfigurationException("Invalid Synapse SQL type: " + type);
        }
    }

    public static String getRoomName() {
        return "select r.name from room_names r, events e, (select r1.room_id,max(e1.origin_server_ts) ts from room_names r1, events e1 where r1.event_id = e1.event_id group by r1.room_id) rle where e.origin_server_ts = rle.ts and r.event_id = e.event_id and r.room_id = ?";
    }

}
