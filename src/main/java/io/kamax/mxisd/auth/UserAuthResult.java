/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
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

package io.kamax.mxisd.auth;

import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.ThreePid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserAuthResult {

    private boolean success;
    private String mxid;
    private String displayName;
    private List<ThreePid> threePids = new ArrayList<>();

    public UserAuthResult failure() {
        success = false;
        mxid = null;
        displayName = null;

        return this;
    }

    public UserAuthResult success(String mxid, String displayName) {
        setSuccess(true);
        setMxid(mxid);
        setDisplayName(displayName);

        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMxid() {
        return mxid;
    }

    public void setMxid(String mxid) {
        this.mxid = mxid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public UserAuthResult withThreePid(ThreePidMedium medium, String address) {
        return withThreePid(medium.getId(), address);
    }

    public UserAuthResult withThreePid(String medium, String address) {
        threePids.add(new ThreePid(medium, address));

        return this;
    }

    public List<ThreePid> getThreePids() {
        return Collections.unmodifiableList(threePids);
    }

}
