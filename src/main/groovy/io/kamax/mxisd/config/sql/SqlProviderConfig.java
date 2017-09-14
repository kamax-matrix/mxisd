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

import javax.annotation.PostConstruct;

@Configuration
@ConfigurationProperties("sql")
public class SqlProviderConfig {

    private Logger log = LoggerFactory.getLogger(SqlProviderConfig.class);

    private boolean enabled;
    private String type;
    private String connection;
    private SqlProviderAuthConfig auth;
    private SqlProviderIdentityConfig identity;

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

    public SqlProviderAuthConfig getAuth() {
        return auth;
    }

    public void setAuth(SqlProviderAuthConfig auth) {
        this.auth = auth;
    }

    public SqlProviderIdentityConfig getIdentity() {
        return identity;
    }

    public void setIdentity(SqlProviderIdentityConfig identity) {
        this.identity = identity;
    }

    @PostConstruct
    private void postConstruct() {
        log.info("--- SQL Provider config ---");
        log.info("Enabled: {}", isEnabled());
        if (isEnabled()) {
            log.info("Type: {}", getType());
            log.info("Connection: {}", getConnection());
            log.info("Auth enabled: {}", getAuth().isEnabled());
            log.info("Identy type: {}", getIdentity().getType());
            log.info("Identity medium queries: {}", new Gson().toJson(getIdentity().getMedium()));
        }
    }

}
