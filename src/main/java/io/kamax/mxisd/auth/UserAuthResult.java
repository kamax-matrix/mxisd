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

package io.kamax.mxisd.auth;

import io.kamax.matrix.ThreePid;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UserAuthResult {

    private boolean success;
    private String displayName;
    private String photo;
    private Set<ThreePid> threePids = new HashSet<>();

    public UserAuthResult failure() {
        success = false;
        displayName = null;
        photo = null;
        threePids.clear();

        return this;
    }

    public UserAuthResult success(String displayName) {
        setSuccess(true);
        setDisplayName(displayName);

        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public UserAuthResult withThreePid(String medium, String address) {
        threePids.add(new ThreePid(medium, address));

        return this;
    }

    public Set<ThreePid> getThreePids() {
        return Collections.unmodifiableSet(threePids);
    }

}
