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

package io.kamax.mxisd.config.sql;

import io.kamax.mxisd.util.GsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class SqlConfig {

    private transient final Logger log = LoggerFactory.getLogger(SqlConfig.class);

    public static class Query {

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

    public static class Type {

        private Query name = new Query();
        private Query threepid = new Query();

        public Query getName() {
            return name;
        }

        public void setName(Query name) {
            this.name = name;
        }

        public Query getThreepid() {
            return threepid;
        }

        public void setThreepid(Query threepid) {
            this.threepid = threepid;
        }

    }

    public static class Auth {

        private Boolean enabled;

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

    }

    public static class Directory {

        private Boolean enabled;
        private Type query = new Type();

        public Directory() {
            query.getName().setType("localpart");
            query.getName().setType("SELECT 1");

            query.getThreepid().setType("localpart");
            query.getThreepid().setType("SELECT 1");
        }

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Type getQuery() {
            return query;
        }

        public void setQuery(Type query) {
            this.query = query;
        }

    }

    public static class Identity {

        private Boolean enabled;
        private String type = "mxid";
        private String query = "SELECT user_id AS uid FROM user_threepids WHERE medium = ? AND address = ?";
        private Map<String, String> medium = new HashMap<>();

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public Map<String, String> getMedium() {
            return medium;
        }

        public void setMedium(Map<String, String> medium) {
            this.medium = medium;
        }

    }

    public static class ProfileDisplayName {

        private String query;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

    }

    public static class ProfileThreepids {

        private String query = "SELECT medium, address FROM user_threepids WHERE user_id = ?";

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

    }

    public static class ProfileRoles {

        private String type;
        private String query;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

    }

    public static class Profile {

        private Boolean enabled;
        private ProfileDisplayName displayName = new ProfileDisplayName();
        private ProfileThreepids threepid = new ProfileThreepids();
        private ProfileRoles role = new ProfileRoles();

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public ProfileDisplayName getDisplayName() {
            return displayName;
        }

        public void setDisplayName(ProfileDisplayName displayName) {
            this.displayName = displayName;
        }

        public ProfileThreepids getThreepid() {
            return threepid;
        }

        public void setThreepid(ProfileThreepids threepid) {
            this.threepid = threepid;
        }

        public ProfileRoles getRole() {
            return role;
        }

        public void setRole(ProfileRoles role) {
            this.role = role;
        }

    }

    private boolean enabled;
    private String type = "sqlite";
    private String connection;
    private Auth auth = new Auth();
    private Directory directory = new Directory();
    private Identity identity = new Identity();
    private Profile profile = new Profile();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
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

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    protected abstract String getProviderName();

    public void build() {
        if (Objects.isNull(getAuth().isEnabled())) {
            getAuth().setEnabled(isEnabled());
        }

        if (Objects.isNull(getDirectory().isEnabled())) {
            getDirectory().setEnabled(isEnabled());
        }

        if (Objects.isNull(getIdentity().isEnabled())) {
            getIdentity().setEnabled(isEnabled());
        }

        if (Objects.isNull(getProfile().isEnabled())) {
            getProfile().setEnabled(isEnabled());
        }
    }

    protected void printConfig() {
        log.info("--- " + getProviderName() + " Provider config ---");

        log.info("Enabled: {}", isEnabled());
        if (isEnabled()) {
            log.info("Type: {}", getType());
            log.info("Has connection info? {}", !StringUtils.isEmpty(getConnection()));
            log.debug("Connection: {}", getConnection());
            log.info("Auth enabled: {}", getAuth().isEnabled());
            log.info("Directory queries: {}", GsonUtil.build().toJson(getDirectory().getQuery()));
            log.info("Identity type: {}", getIdentity().getType());
            log.info("3PID mapping query: {}", getIdentity().getQuery());
            log.info("Identity medium queries: {}", GsonUtil.build().toJson(getIdentity().getMedium()));
            log.info("Profile:");
            log.info("  Enabled: {}", getProfile().isEnabled());
            if (getProfile().isEnabled()) {
                log.info("  Display name query: {}", getProfile().getDisplayName().getQuery());
                log.info("  Profile 3PID query: {}", getProfile().getThreepid().getQuery());
                log.info("  Role query: {}", getProfile().getRole().getQuery());
            }
        }
    }

}
