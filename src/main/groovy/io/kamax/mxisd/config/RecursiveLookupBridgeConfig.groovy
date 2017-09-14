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

package io.kamax.mxisd.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "lookup.recursive.bridge")
class RecursiveLookupBridgeConfig implements InitializingBean {

    private Logger log = LoggerFactory.getLogger(RecursiveLookupBridgeConfig.class)

    private boolean enabled
    private boolean recursiveOnly
    private String server
    private Map<String, String> mappings = new HashMap<>()

    boolean getEnabled() {
        return enabled
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled
    }

    boolean getRecursiveOnly() {
        return recursiveOnly
    }

    void setRecursiveOnly(boolean recursiveOnly) {
        this.recursiveOnly = recursiveOnly
    }

    String getServer() {
        return server
    }

    void setServer(String server) {
        this.server = server
    }

    Map<String, String> getMappings() {
        return mappings
    }

    void setMappings(Map<String, String> mappings) {
        this.mappings = mappings
    }

    @Override
    void afterPropertiesSet() throws Exception {
        log.info("--- Bridge integration lookups config ---")
        log.info("Enabled: {}", getEnabled())
        if (getEnabled()) {
            log.info("Recursive only: {}", getRecursiveOnly())
            log.info("Fallback Server: {}", getServer())
            log.info("Mappings: {}", mappings.size())
        }
    }

}
