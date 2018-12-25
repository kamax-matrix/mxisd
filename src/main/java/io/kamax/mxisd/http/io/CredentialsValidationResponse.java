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

package io.kamax.mxisd.http.io;

import io.kamax.matrix.ThreePid;

import java.util.HashSet;
import java.util.Set;

public class CredentialsValidationResponse {

    public static class Profile {

        private String displayName;
        private Set<ThreePid> threePids = new HashSet<>();

        public String getDisplayName() {
            return displayName;
        }

        public Set<ThreePid> getThreePids() {
            return threePids;
        }

        public void setThreePids(Set<ThreePid> threePids) {
            this.threePids = new HashSet<>(threePids);
        }

    }

    private boolean success;
    private String displayName; // TODO remove later, legacy support
    private Profile profile = new Profile();

    public CredentialsValidationResponse(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.profile.displayName = displayName;
    }

    public Profile getProfile() {
        return profile;
    }

}
