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

package io.kamax.mxisd.auth.provider;

import io.kamax.mxisd.ThreePid;
import io.kamax.mxisd.UserID;
import io.kamax.mxisd.UserIdType;

import java.util.ArrayList;
import java.util.List;

public class BackendAuthResult {

    public static class BackendAuthProfile {

        private String displayName;
        private List<ThreePid> threePids = new ArrayList<>();

        public String getDisplayName() {
            return displayName;
        }

        public List<ThreePid> getThreePids() {
            return threePids;
        }
    }

    public static BackendAuthResult failure() {
        BackendAuthResult r = new BackendAuthResult();
        r.success = false;
        return r;
    }

    public static BackendAuthResult success(String id, UserIdType type, String displayName) {
        return success(id, type.getId(), displayName);
    }

    public static BackendAuthResult success(String id, String type, String displayName) {
        BackendAuthResult r = new BackendAuthResult();
        r.success = true;
        r.id = new UserID(type, id);
        r.profile = new BackendAuthProfile();
        r.profile.displayName = displayName;

        return r;
    }

    private Boolean success;
    private UserID id;
    private BackendAuthProfile profile = new BackendAuthProfile();

    public Boolean isSuccess() {
        return success;
    }

    public UserID getId() {
        return id;
    }

    public BackendAuthProfile getProfile() {
        return profile;
    }

    public BackendAuthResult withThreePid(ThreePid threePid) {
        this.profile.threePids.add(threePid);

        return this;
    }

}
