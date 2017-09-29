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

package io.kamax.mxisd.config.sql;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties("sql")
@Primary
public class SqlProviderConfig {

    private Logger log = LoggerFactory.getLogger(SqlProviderConfig.class);

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
        private String type;
        private String query;
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

    private boolean enabled;
    private String type;
    private String connection;
    private Auth auth = new Auth();
    private Directory directory = new Directory();
    private Identity identity = new Identity();

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

    @PostConstruct
    public void build() {
        log.info("--- SQL Provider config ---");

        if (getAuth().isEnabled() == null) {
            getAuth().setEnabled(isEnabled());
        }

        if (getDirectory().isEnabled() == null) {
            getDirectory().setEnabled(isEnabled());
        }

        if (getIdentity().isEnabled() == null) {
            getIdentity().setEnabled(isEnabled());
        }

        log.info("Enabled: {}", isEnabled());
        if (isEnabled()) {
            log.info("Type: {}", getType());
            log.info("Connection: {}", getConnection());
            log.info("Auth enabled: {}", getAuth().isEnabled());
            log.info("Identity type: {}", getIdentity().getType());
            log.info("Identity medium queries: {}", new Gson().toJson(getIdentity().getMedium()));
        }
    }

}
