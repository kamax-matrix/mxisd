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

package io.kamax.mxisd.config;

import java.util.ArrayList;
import java.util.List;

public class RecursiveLookupConfig {

    private boolean enabled = true;
    private List<String> allowedCidr = new ArrayList<>();
    private RecursiveLookupBridgeConfig bridge = new RecursiveLookupBridgeConfig();

    public RecursiveLookupConfig() {
        allowedCidr.add("127.0.0.0/8");
        allowedCidr.add("10.0.0.0/8");
        allowedCidr.add("172.16.0.0/12");
        allowedCidr.add("192.168.0.0/16");
        allowedCidr.add("::1/128");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getAllowedCidr() {
        return allowedCidr;
    }

    public void setAllowedCidr(List<String> allowedCidr) {
        this.allowedCidr = allowedCidr;
    }

    public RecursiveLookupBridgeConfig getBridge() {
        return bridge;
    }

    public void setBridge(RecursiveLookupBridgeConfig bridge) {
        this.bridge = bridge;
    }

    public void build() {
        bridge.build();
    }

}
