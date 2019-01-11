/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sàrl
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

package io.kamax.mxisd.config.threepid;

import io.kamax.matrix.ThreePidMedium;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.threepid.medium.EmailConfig;
import io.kamax.mxisd.config.threepid.medium.PhoneConfig;

import java.util.HashMap;
import java.util.Map;

public class ThreePidConfig {

    private Map<String, Object> medium = new HashMap<>();

    public ThreePidConfig() { // TODO Check if this is still needed
        medium.put(ThreePidMedium.Email.getId(), GsonUtil.makeObj(new EmailConfig()));
        medium.put(ThreePidMedium.PhoneNumber.getId(), GsonUtil.makeObj(new PhoneConfig()));
    }

    public Map<String, Object> getMedium() {
        return medium;
    }

    public void setMedium(Map<String, Object> medium) {
        this.medium = medium;
    }

    public void build() {
        // no-op
    }

}
