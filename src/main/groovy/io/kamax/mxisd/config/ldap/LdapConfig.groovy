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

package io.kamax.mxisd.config.ldap

import groovy.json.JsonOutput
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

import javax.annotation.PostConstruct

@Configuration
@ConfigurationProperties(prefix = "ldap")
class LdapConfig {

    private Logger log = LoggerFactory.getLogger(LdapConfig.class)

    private boolean enabled

    @Autowired
    private LdapConnectionConfig conn
    private LdapAttributeConfig attribute
    private LdapAuthConfig auth
    private LdapIdentityConfig identity

    boolean isEnabled() {
        return enabled
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled
    }

    LdapConnectionConfig getConn() {
        return conn
    }

    void setConn(LdapConnectionConfig conn) {
        this.conn = conn
    }

    LdapAttributeConfig getAttribute() {
        return attribute
    }

    void setAttribute(LdapAttributeConfig attribute) {
        this.attribute = attribute
    }

    LdapAuthConfig getAuth() {
        return auth
    }

    void setAuth(LdapAuthConfig auth) {
        this.auth = auth
    }

    LdapIdentityConfig getIdentity() {
        return identity
    }

    void setIdentity(LdapIdentityConfig identity) {
        this.identity = identity
    }

    @PostConstruct
    void afterPropertiesSet() {
        log.info("--- LDAP Config ---")
        log.info("Enabled: {}", isEnabled())

        if (!isEnabled()) {
            return
        }

        if (StringUtils.isBlank(conn.getHost())) {
            throw new IllegalStateException("LDAP Host must be configured!")
        }

        if (1 > conn.getPort() || 65535 < conn.getPort()) {
            throw new IllegalStateException("LDAP port is not valid")
        }

        if (StringUtils.isBlank(attribute.getUid().getType())) {
            throw new IllegalStateException("Attribute UID Type cannot be empty")
        }


        if (StringUtils.isBlank(attribute.getUid().getValue())) {
            throw new IllegalStateException("Attribute UID value cannot be empty")
        }


        log.info("Conn: {}", JsonOutput.toJson(conn))
        log.info("Host: {}", conn.getHost())
        log.info("Port: {}", conn.getPort())
        log.info("Bind DN: {}", conn.getBindDn())
        log.info("Base DN: {}", conn.getBaseDn())

        log.info("Attribute: {}", JsonOutput.toJson(attribute))
        log.info("Auth: {}", JsonOutput.toJson(auth))
        log.info("Identity: {}", JsonOutput.toJson(identity))
    }

}
