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

package io.kamax.mxisd.config.ldap;

import com.google.gson.Gson;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.backend.ldap.LdapGenericBackend;
import io.kamax.mxisd.exception.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "ldap")
public class LdapConfig {

    private Logger log = LoggerFactory.getLogger(LdapConfig.class);
    private static Gson gson = new Gson();

    private boolean enabled;
    private String filter;

    public static class Directory {

        public static class Attribute {

            private List<String> other = new ArrayList<>();

            public List<String> getOther() {
                return other;
            }

            public void setOther(List<String> other) {
                this.other = other;
            }

        }

        private Attribute attribute = new Attribute();
        private String filter;

        public Attribute getAttribute() {
            return attribute;
        }

        public void setAttribute(Attribute attribute) {
            this.attribute = attribute;
        }

        public String getFilter() {
            return filter;
        }

        public void setFilter(String filter) {
            this.filter = filter;
        }

    }

    @Autowired
    private LdapConnectionConfig conn;
    private LdapAttributeConfig attribute;
    private LdapAuthConfig auth;
    private Directory directory;
    private LdapIdentityConfig identity;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public LdapConnectionConfig getConn() {
        return conn;
    }

    public void setConn(LdapConnectionConfig conn) {
        this.conn = conn;
    }

    public LdapAttributeConfig getAttribute() {
        return attribute;
    }

    public void setAttribute(LdapAttributeConfig attribute) {
        this.attribute = attribute;
    }

    public LdapAuthConfig getAuth() {
        return auth;
    }

    public void setAuth(LdapAuthConfig auth) {
        this.auth = auth;
    }

    public Directory getDirectory() {
        return directory;
    }

    public void setDirectory(Directory directory) {
        this.directory = directory;
    }

    public LdapIdentityConfig getIdentity() {
        return identity;
    }

    public void setIdentity(LdapIdentityConfig identity) {
        this.identity = identity;
    }

    @PostConstruct
    public void build() {
        log.info("--- LDAP Config ---");
        log.info("Enabled: {}", isEnabled());

        if (!isEnabled()) {
            return;
        }

        if (StringUtils.isBlank(conn.getHost())) {
            throw new IllegalStateException("LDAP Host must be configured!");
        }

        if (conn.getPort() < 1 || conn.getPort() > 65535) {
            throw new IllegalStateException("LDAP port is not valid");
        }

        if (StringUtils.isBlank(attribute.getUid().getType())) {
            throw new IllegalStateException("Attribute UID Type cannot be empty");
        }


        if (StringUtils.isBlank(attribute.getUid().getValue())) {
            throw new IllegalStateException("Attribute UID value cannot be empty");
        }

        String uidType = attribute.getUid().getType();
        if (!StringUtils.equals(LdapGenericBackend.UID, uidType) && !StringUtils.equals(LdapGenericBackend.MATRIX_ID, uidType)) {
            throw new IllegalArgumentException("Unsupported LDAP UID type: " + uidType);
        }

        if (StringUtils.isBlank(identity.getToken())) {
            throw new ConfigurationException("ldap.identity.token");
        }

        // Build queries
        attribute.getThreepid().forEach((k, v) -> {
            if (StringUtils.isBlank(identity.getMedium().get(k))) {
                if (ThreePidMedium.PhoneNumber.is(k)) {
                    identity.getMedium().put(k, LdapGenericBackend.buildOrQuery("+" + getIdentity().getToken(), v));
                } else {
                    identity.getMedium().put(k, LdapGenericBackend.buildOrQuery(getIdentity().getToken(), v));
                }
            }
        });

        getAuth().setFilter(StringUtils.defaultIfBlank(getAuth().getFilter(), getFilter()));
        getDirectory().setFilter(StringUtils.defaultIfBlank(getDirectory().getFilter(), getFilter()));
        getIdentity().setFilter(StringUtils.defaultIfBlank(getIdentity().getFilter(), getFilter()));

        log.info("Host: {}", conn.getHost());
        log.info("Port: {}", conn.getPort());
        log.info("Bind DN: {}", conn.getBindDn());
        log.info("Base DN: {}", conn.getBaseDn());

        log.info("Attribute: {}", gson.toJson(attribute));
        log.info("Auth: {}", gson.toJson(auth));
        log.info("Directory: {}", gson.toJson(directory));
        log.info("Identity: {}", gson.toJson(identity));
    }

}
