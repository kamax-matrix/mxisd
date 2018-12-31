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

package io.kamax.mxisd.config.wordpress;

import io.kamax.mxisd.exception.ConfigurationException;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class WordpressConfig {

    public static class Credential {

        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

    }

    public static class Rest {

        private Credential credential = new Credential();
        private String base;

        public String getBase() {
            return base;
        }

        public void setBase(String base) {
            this.base = base;
        }

        public Credential getCredential() {
            return credential;
        }

        public void setCredential(Credential credential) {
            this.credential = credential;
        }

    }

    public static class Query {

        private Map<String, String> threepid = new HashMap<>();
        private Map<String, String> directory = new HashMap<>();

        public Query() {
            threepid.put("email", "SELECT user_login as uid FROM %TABLE_PREFIX%users WHERE user_email = ?");
            directory.put("name", "SELECT DISTINCT user_login, display_name FROM %TABLE_PREFIX%users u LEFT JOIN %TABLE_PREFIX%usermeta m ON m.user_id = u.id WHERE u.display_name LIKE ? OR (m.meta_key = 'nickname' AND m.meta_value = ?) OR (m.meta_key = 'first_name' AND m.meta_value = ?) OR (m.meta_key = 'last_name' AND m.meta_value = ?)");
            directory.put("threepid", "SELECT DISTINCT user_login, display_name FROM %TABLE_PREFIX%users WHERE user_email LIKE ?");
        }

        public Map<String, String> getThreepid() {
            return threepid;
        }

        public void setThreepid(Map<String, String> threepid) {
            this.threepid = threepid;
        }

        public Map<String, String> getDirectory() {
            return directory;
        }

        public void setDirectory(Map<String, String> directory) {
            this.directory = directory;
        }

        public void build() {
            // FIXME replace table prefix
        }

    }

    public static class Sql {

        private String type = "mysql";
        private String connection;
        private String tablePrefix = "wp_";
        private Query query = new Query();

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

        public String getTablePrefix() {
            return tablePrefix;
        }

        public void setTablePrefix(String tablePrefix) {
            this.tablePrefix = tablePrefix;
        }

        public Query getQuery() {
            return query;
        }

        public void setQuery(Query query) {
            this.query = query;
        }

    }

    private boolean enabled;
    private Rest rest = new Rest();
    private Sql sql = new Sql();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Rest getRest() {
        return rest;
    }

    public void setRest(Rest rest) {
        this.rest = rest;
    }

    public Sql getSql() {
        return sql;
    }

    public void setSql(Sql sql) {
        this.sql = sql;
    }

    public void build() {
        if (!isEnabled()) {
            return;
        }

        if (StringUtils.isBlank(getRest().getBase())) {
            throw new ConfigurationException("wordpress.rest.base");
        }
    }

}
