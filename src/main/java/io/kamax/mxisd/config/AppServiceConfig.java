/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
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

package io.kamax.mxisd.config;

import io.kamax.mxisd.Mxisd;
import io.kamax.mxisd.exception.ConfigurationException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AppServiceConfig {

    public static class Users {

        private String main = "mxisd";
        private String inviteExpired = "_mxisd_invite-expired";

        public String getMain() {
            return main;
        }

        public void setMain(String main) {
            this.main = main;
        }

        public String getInviteExpired() {
            return inviteExpired;
        }

        public void setInviteExpired(String inviteExpired) {
            this.inviteExpired = inviteExpired;
        }

        public void build() {
            // no-op
        }

    }

    public static class Endpoint {

        private String url;
        private String token;

        private transient URL cUrl;

        public URL getUrl() {
            return cUrl;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public void build() {
            if (Objects.isNull(url)) {
                return;
            }

            try {
                cUrl = new URL(url);
            } catch (MalformedURLException e) {
                throw new ConfigurationException("AppService endpoint(s) URL definition");
            }
        }

    }

    public static class Endpoints {

        private Endpoint toAS = new Endpoint();
        private Endpoint toHS = new Endpoint();

        public Endpoint getToAS() {
            return toAS;
        }

        public void setToAS(Endpoint toAS) {
            this.toAS = toAS;
        }

        public Endpoint getToHS() {
            return toHS;
        }

        public void setToHS(Endpoint toHS) {
            this.toHS = toHS;
        }

        public void build() {
            toAS.build();
            toHS.build();
        }

    }

    public static class Synapse {

        private String id = "appservice-" + Mxisd.Name;
        private String file;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public void build() {
            // no-op
        }

    }

    public static class Registration {

        private Synapse synapse = new Synapse();

        public Synapse getSynapse() {
            return synapse;
        }

        public void setSynapse(Synapse synapse) {
            this.synapse = synapse;
        }

        public void build() {
            synapse.build();
        }

    }

    public static class AdminFeature {

        private Boolean enabled;
        private List<String> allowedRoles = new ArrayList<>();

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getAllowedRoles() {
            return allowedRoles;
        }

        public void setAllowedRoles(List<String> allowedRoles) {
            this.allowedRoles = allowedRoles;
        }

        public void build() {
            // no-op
        }

    }

    public static class Features {

        private AdminFeature admin = new AdminFeature();
        private Boolean inviteById;
        private Boolean cleanExpiredInvite;

        public AdminFeature getAdmin() {
            return admin;
        }

        public void setAdmin(AdminFeature admin) {
            this.admin = admin;
        }

        public Boolean getInviteById() {
            return inviteById;
        }

        public void setInviteById(Boolean inviteById) {
            this.inviteById = inviteById;
        }

        public Boolean getCleanExpiredInvite() {
            return cleanExpiredInvite;
        }

        public void setCleanExpiredInvite(Boolean cleanExpiredInvite) {
            this.cleanExpiredInvite = cleanExpiredInvite;
        }

        public void build() {
            admin.build();
        }

    }

    private Boolean enabled;
    private Features feature = new Features();
    private Endpoints endpoint = new Endpoints();
    private Registration registration = new Registration();
    private Users user = new Users();

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Features getFeature() {
        return feature;
    }

    public void setFeature(Features feature) {
        this.feature = feature;
    }

    public Endpoints getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoints endpoint) {
        this.endpoint = endpoint;
    }

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public void build() {
        endpoint.build();
        feature.build();
        registration.build();
        user.build();
    }

}
