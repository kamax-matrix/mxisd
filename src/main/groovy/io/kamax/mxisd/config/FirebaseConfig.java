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

package io.kamax.mxisd.config;

import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.auth.provider.GoogleFirebaseAuthenticator;
import io.kamax.mxisd.invitation.InvitationManager;
import io.kamax.mxisd.lookup.provider.GoogleFirebaseProvider;
import io.kamax.mxisd.lookup.provider.IThreePidProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@ConfigurationProperties("firebase")
public class FirebaseConfig {

    private Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Autowired
    private ServerConfig srvCfg;

    @Autowired
    private InvitationManager invMgr;

    private boolean enabled;
    private String credentials;
    private String database;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    @PostConstruct
    private void postConstruct() {
        log.info("--- Firebase configuration ---");
        log.info("Enabled: {}", isEnabled());
        if (isEnabled()) {
            log.info("Credentials: {}", getCredentials());
            log.info("Database: {}", getDatabase());
        }
    }

    @Bean
    public AuthenticatorProvider getAuthProvider() {
        if (!enabled) {
            return new GoogleFirebaseAuthenticator(invMgr, false);
        } else {
            return new GoogleFirebaseAuthenticator(invMgr, credentials, database, srvCfg.getName());
        }
    }

    @Bean
    public IThreePidProvider getLookupProvider() {
        if (!enabled) {
            return new GoogleFirebaseProvider(false);
        } else {
            return new GoogleFirebaseProvider(credentials, database, srvCfg.getName());
        }
    }

}
