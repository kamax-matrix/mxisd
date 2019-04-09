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

package io.kamax.mxisd.storage.ormlite.dao;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import io.kamax.mxisd.invitation.IThreePidInviteReply;

import java.time.Instant;

@DatabaseTable(tableName = "invite_3pid_history")
public class HistoricalThreePidInviteIO extends ThreePidInviteIO {

    @DatabaseField(canBeNull = false)
    private String resolvedTo;

    @DatabaseField(canBeNull = false)
    private long resolvedAt;

    @DatabaseField(canBeNull = false)
    private boolean couldPublish;

    @DatabaseField(canBeNull = false)
    private long publishAttempts = 1; // Placeholder for retry mechanism, if ever implemented

    public HistoricalThreePidInviteIO() {
        // Needed for ORMLite
    }

    public HistoricalThreePidInviteIO(IThreePidInviteReply data, String resolvedTo, Instant resolvedAt, boolean couldPublish) {
        super(data);

        this.resolvedTo = resolvedTo;
        this.resolvedAt = resolvedAt.toEpochMilli();
        this.couldPublish = couldPublish;
    }

    public String getResolvedTo() {
        return resolvedTo;
    }

    public Instant getResolvedAt() {
        return Instant.ofEpochMilli(resolvedAt);
    }

    public boolean isCouldPublish() {
        return couldPublish;
    }

    public long getPublishAttempts() {
        return publishAttempts;
    }

}
