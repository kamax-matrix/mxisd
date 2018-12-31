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

package io.kamax.mxisd.config.rest;

import io.kamax.mxisd.exception.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

public class RestBackendConfig {

    public static class IdentityEndpoints {

        private String single = "/_mxisd/backend/api/v1/identity/lookup/single";
        private String bulk = "/_mxisd/backend/api/v1/identity/lookup/bulk";

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

    public static class ProfileEndpoints {

        private String displayName = "/_mxisd/backend/api/v1/profile/displayName";
        private String threepids = "/_mxisd/backend/api/v1/profile/threepids";
        private String roles = "/_mxisd/backend/api/v1/profile/roles";

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getThreepids() {
            return threepids;
        }

        public void setThreepids(String threepids) {
            this.threepids = threepids;
        }

        public String getRoles() {
            return roles;
        }

        public void setRoles(String roles) {
            this.roles = roles;
        }

    }

    public static class Endpoints {

        private String auth = "/_mxisd/backend/api/v1/auth/login";
        private String directory = "/_mxisd/backend/api/v1/directory/user/search";
        private IdentityEndpoints identity = new IdentityEndpoints();
        private ProfileEndpoints profile = new ProfileEndpoints();

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }

        public IdentityEndpoints getIdentity() {
            return identity;
        }

        public void setIdentity(IdentityEndpoints identity) {
            this.identity = identity;
        }

        public Optional<ProfileEndpoints> getProfile() {
            return Optional.ofNullable(profile);
        }

        public void setProfile(ProfileEndpoints profile) {
            this.profile = profile;
        }

    }

    private transient final Logger log = LoggerFactory.getLogger(RestBackendConfig.class);

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
        if (!StringUtils.startsWith(endpoint, "/")) {
            return endpoint;
        }

        if (StringUtils.isBlank(getHost())) {
            throw new ConfigurationException("rest.host");
        }

        try {
            new URL(getHost());
        } catch (MalformedURLException e) {
            throw new ConfigurationException("rest.host", e.getMessage());
        }

        return getHost() + endpoint;
    }

    public void build() {
        log.info("--- REST backend config ---");
        log.info("Enabled: {}", isEnabled());

        if (isEnabled()) {
            endpoints.setAuth(buildEndpointUrl(endpoints.getAuth()));
            endpoints.setDirectory(buildEndpointUrl(endpoints.getDirectory()));
            endpoints.identity.setSingle(buildEndpointUrl(endpoints.identity.getSingle()));
            endpoints.identity.setBulk(buildEndpointUrl(endpoints.identity.getBulk()));

            if (Objects.nonNull(endpoints.profile)) {
                endpoints.profile.setDisplayName(buildEndpointUrl(endpoints.profile.getDisplayName()));
                endpoints.profile.setThreepids(buildEndpointUrl(endpoints.profile.getThreepids()));
                endpoints.profile.setRoles(buildEndpointUrl(endpoints.profile.getRoles()));
            }

            log.info("Host: {}", getHost());
            log.info("Auth endpoint: {}", endpoints.getAuth());
            log.info("Directory endpoint: {}", endpoints.getDirectory());
            log.info("Identity Single endpoint: {}", endpoints.identity.getSingle());
            log.info("Identity Bulk endpoint: {}", endpoints.identity.getBulk());
        }
    }

}
