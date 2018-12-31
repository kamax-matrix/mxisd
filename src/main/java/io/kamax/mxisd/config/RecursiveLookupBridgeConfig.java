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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class RecursiveLookupBridgeConfig {

    private transient final Logger log = LoggerFactory.getLogger(RecursiveLookupBridgeConfig.class);

    private boolean enabled;
    private boolean recursiveOnly = true;
    private String server;
    private Map<String, String> mappings = new HashMap<>();

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean getRecursiveOnly() {
        return recursiveOnly;
    }

    public void setRecursiveOnly(boolean recursiveOnly) {
        this.recursiveOnly = recursiveOnly;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public Map<String, String> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, String> mappings) {
        this.mappings = mappings;
    }

    public void build() {
        log.info("--- Bridge integration lookups config ---");
        log.info("Enabled: {}", getEnabled());
        if (getEnabled()) {
            log.info("Recursive only: {}", getRecursiveOnly());
            log.info("Fallback Server: {}", getServer());
            log.info("Mappings: {}", mappings.size());
        }
    }

}
