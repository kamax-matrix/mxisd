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

package io.kamax.mxisd.config.rest;

import io.kamax.mxisd.exception.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;

@Configuration
@ConfigurationProperties("rest")
public class RestBackendConfig {

    public static class IdentityEndpoints {

        private String single;
        private String bulk;

        public String getSingle() {
            return single;
        }

        public void setSingle(String single) {
            this.single = single;
        }

        public String getBulk() {
            return bulk;
        }

        public void setBulk(String bulk) {
            this.bulk = bulk;
        }

    }

    public static class Endpoints {

        private IdentityEndpoints identity = new IdentityEndpoints();
        private String auth;

        public IdentityEndpoints getIdentity() {
            return identity;
        }

        public void setIdentity(IdentityEndpoints identity) {
            this.identity = identity;
        }

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }

    }

    private Logger log = LoggerFactory.getLogger(RestBackendConfig.class);

    private boolean enabled;
    private String host;
    private Endpoints endpoints = new Endpoints();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Endpoints getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Endpoints endpoints) {
        this.endpoints = endpoints;
    }

    private String buildEndpointUrl(String endpoint) {
        if (StringUtils.startsWith(endpoint, "/")) {
            if (StringUtils.isBlank(getHost())) {
                throw new ConfigurationException("rest.host");
            }

            try {
                new URL(getHost());
            } catch (MalformedURLException e) {
                throw new ConfigurationException("rest.host", e.getMessage());
            }

            return getHost() + endpoint;
        } else {
            return endpoint;
        }
    }

    @PostConstruct
    private void postConstruct() {
        log.info("--- REST backend config ---");
        log.info("Enabled: {}", isEnabled());

        if (isEnabled()) {
            endpoints.setAuth(buildEndpointUrl(endpoints.getAuth()));
            endpoints.identity.setSingle(buildEndpointUrl(endpoints.identity.getSingle()));
            endpoints.identity.setBulk(buildEndpointUrl(endpoints.identity.getBulk()));

            log.info("Host: {}", getHost());
            log.info("Auth endpoint: {}", endpoints.getAuth());
            log.info("Identity Single endpoint: {}", endpoints.identity.getSingle());
            log.info("Identity Bulk endpoint: {}", endpoints.identity.getBulk());
        }
    }

}
