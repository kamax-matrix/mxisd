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

package io.kamax.mxisd.profile;

import io.kamax.matrix.ThreePid;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JsonProfileResult {

    private String displayName;
    private List<ThreePid> threepids;
    private List<String> roles;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<ThreePid> getThreepids() {
        return threepids;
    }

    public void setThreepids(List<ThreePid> threepids) {
        this.threepids = threepids;
    }

    public void addThreepid(ThreePid threepid) {
        if (Objects.isNull(threepids)) threepids = new ArrayList<>();
        threepids.add(threepid);
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public void addRole(String role) {
        if (Objects.isNull(roles)) roles = new ArrayList<>();
        roles.add(role);
    }

}
