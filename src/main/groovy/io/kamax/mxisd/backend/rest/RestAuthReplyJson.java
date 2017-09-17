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

package io.kamax.mxisd.backend.rest;

import io.kamax.mxisd.ThreePid;

import java.util.ArrayList;
import java.util.List;

public class RestAuthReplyJson {

    public static class RestAuthProfileData {

        private String displayName;
        private List<ThreePid> threePids = new ArrayList<>();

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public List<ThreePid> getThreePids() {
            return threePids;
        }

        public void setThreePids(List<ThreePid> threePids) {
            this.threePids = threePids;
        }

    }

    private Boolean success;
    private String mxid;
    private RestAuthProfileData profile;

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

    public RestAuthProfileData getProfile() {
        return profile;
    }

    public void setProfile(RestAuthProfileData profile) {
        this.profile = profile;
    }

}
