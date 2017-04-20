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
@ConfigurationProperties(prefix = "ldap")
class LdapConfig implements InitializingBean {

    private Logger log = LoggerFactory.getLogger(LdapConfig.class)

    private String host
    private int port
    private String baseDn
    private String type
    private String attribute
    private String bindDn
    private String bindPassword
    private Map<String, String> mappings

    String getHost() {
        return host
    }

    void setHost(String host) {
        this.host = host
    }

    int getPort() {
        return port
    }

    void setPort(int port) {
        this.port = port
    }

    String getBaseDn() {
        return baseDn
    }

    void setBaseDn(String baseDn) {
        this.baseDn = baseDn
    }

    String getType() {
        return type
    }

    void setType(String type) {
        this.type = type
    }

    String getAttribute() {
        return attribute
    }

    void setAttribute(String attribute) {
        this.attribute = attribute
    }

    String getBindDn() {
        return bindDn
    }

    void setBindDn(String bindDn) {
        this.bindDn = bindDn
    }

    String getBindPassword() {
        return bindPassword
    }

    void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword
    }

    Map<String, String> getMappings() {
        return mappings
    }

    void setMappings(Map<String, String> mappings) {
        this.mappings = mappings
    }

    Optional<String> getMapping(String type) {
        if (mappings == null) {
            return Optional.empty()
        }

        return Optional.ofNullable(mappings.get(type))
    }

    @Override
    void afterPropertiesSet() throws Exception {
        log.info("Matrix ID type: {}", getType())
    }

}
