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
import io.kamax.mxisd.backend.ldap.LdapBackend;
import io.kamax.mxisd.exception.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.*;

@Configuration
@ConfigurationProperties(prefix = "ldap")
public class LdapConfig {

    public static class UID {

        private String type;
        private String value;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

    public static class Attribute {

        private UID uid;
        private String name;
        private Map<String, List<String>> threepid = new HashMap<>();

        public UID getUid() {
            return uid;
        }

        public void setUid(UID uid) {
            this.uid = uid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, List<String>> getThreepid() {
            return threepid;
        }

        public void setThreepid(Map<String, List<String>> threepid) {
            this.threepid = threepid;
        }

    }

    public static class Auth {

        private String filter;

        public String getFilter() {
            return filter;
        }

        public void setFilter(String filter) {
            this.filter = filter;
        }

    }

    public static class Connection {

        private boolean tls;
        private String host;
        private int port;
        private String bindDn;
        private String bindPassword;
        private String baseDn;

        public boolean isTls() {
            return tls;
        }

        public void setTls(boolean tls) {
            this.tls = tls;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getBindDn() {
            return bindDn;
        }

        public void setBindDn(String bindDn) {
            this.bindDn = bindDn;
        }

        public String getBindPassword() {
            return bindPassword;
        }

        public void setBindPassword(String bindPassword) {
            this.bindPassword = bindPassword;
        }

        public String getBaseDn() {
            return baseDn;
        }

        public void setBaseDn(String baseDn) {
            this.baseDn = baseDn;
        }

    }

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

    public static class Identity {

        private String filter;
        private String token;
        private Map<String, String> medium = new HashMap<>();

        public String getFilter() {
            return filter;
        }

        public void setFilter(String filter) {
            this.filter = filter;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Map<String, String> getMedium() {
            return medium;
        }

        public Optional<String> getQuery(String key) {
            return Optional.ofNullable(medium.get(key));
        }

        public void setMedium(Map<String, String> medium) {
            this.medium = medium;
        }

    }


    private Logger log = LoggerFactory.getLogger(LdapConfig.class);
    private static Gson gson = new Gson();

    private boolean enabled;
    private String filter;

    private Connection connection;
    private Attribute attribute;
    private Auth auth;
    private Directory directory;
    private Identity identity;

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

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection conn) {
        this.connection = conn;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Directory getDirectory() {
        return directory;
    }

    public void setDirectory(Directory directory) {
        this.directory = directory;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    @PostConstruct
    public void build() {
        log.info("--- LDAP Config ---");
        log.info("Enabled: {}", isEnabled());

        if (!isEnabled()) {
            return;
        }

        if (StringUtils.isBlank(connection.getHost())) {
            throw new IllegalStateException("LDAP Host must be configured!");
        }

        if (connection.getPort() < 1 || connection.getPort() > 65535) {
            throw new IllegalStateException("LDAP port is not valid");
        }

        if (StringUtils.isBlank(connection.getBaseDn())) {
            throw new ConfigurationException("ldap.connection.baseDn");
        }

        if (StringUtils.isBlank(attribute.getUid().getType())) {
            throw new IllegalStateException("Attribute UID Type cannot be empty");
        }

        if (StringUtils.isBlank(attribute.getUid().getValue())) {
            throw new IllegalStateException("Attribute UID value cannot be empty");
        }

        String uidType = attribute.getUid().getType();
        if (!StringUtils.equals(LdapBackend.UID, uidType) && !StringUtils.equals(LdapBackend.MATRIX_ID, uidType)) {
            throw new IllegalArgumentException("Unsupported LDAP UID type: " + uidType);
        }

        if (StringUtils.isBlank(identity.getToken())) {
            throw new ConfigurationException("ldap.identity.token");
        }

        // Build queries
        attribute.getThreepid().forEach((k, v) -> {
            if (StringUtils.isBlank(identity.getMedium().get(k))) {
                if (ThreePidMedium.PhoneNumber.is(k)) {
                    identity.getMedium().put(k, LdapBackend.buildOrQuery("+" + getIdentity().getToken(), v));
                } else {
                    identity.getMedium().put(k, LdapBackend.buildOrQuery(getIdentity().getToken(), v));
                }
            }
        });

        getAuth().setFilter(StringUtils.defaultIfBlank(getAuth().getFilter(), getFilter()));
        getDirectory().setFilter(StringUtils.defaultIfBlank(getDirectory().getFilter(), getFilter()));
        getIdentity().setFilter(StringUtils.defaultIfBlank(getIdentity().getFilter(), getFilter()));

        log.info("Host: {}", connection.getHost());
        log.info("Port: {}", connection.getPort());
        log.info("Bind DN: {}", connection.getBindDn());
        log.info("Base DN: {}", connection.getBaseDn());

        log.info("Attribute: {}", gson.toJson(attribute));
        log.info("Auth: {}", gson.toJson(auth));
        log.info("Directory: {}", gson.toJson(directory));
        log.info("Identity: {}", gson.toJson(identity));
    }

}
