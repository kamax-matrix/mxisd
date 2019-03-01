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

import io.kamax.mxisd.exception.ConfigurationException;
import org.apache.commons.lang.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ListenerConfig {

    public static class Synpase {

        private String registrationFile;

        public String getRegistrationFile() {
            return registrationFile;
        }

        public void setRegistrationFile(String registrationFile) {
            this.registrationFile = registrationFile;
        }

    }

    public static class UserTemplate {

        private String type = "regex";
        private String template;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }

    }

    public static class Token {

        private String as;
        private String hs;

        public String getAs() {
            return as;
        }

        public void setAs(String as) {
            this.as = as;
        }

        public String getHs() {
            return hs;
        }

        public void setHs(String hs) {
            this.hs = hs;
        }

    }

    private String id = "appservice-mxisd";
    private String url;
    private String localpart = "mxisd";
    private Token token = new Token();
    private List<UserTemplate> users = new ArrayList<>();
    private Synpase synapse = new Synpase();

    private transient URL csUrl;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public URL getUrl() {
        return csUrl;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLocalpart() {
        return localpart;
    }

    public void setLocalpart(String localpart) {
        this.localpart = localpart;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public List<UserTemplate> getUsers() {
        return users;
    }

    public void setUsers(List<UserTemplate> users) {
        this.users = users;
    }

    public Synpase getSynapse() {
        return synapse;
    }

    public void setSynapse(Synpase synapse) {
        this.synapse = synapse;
    }

    public void build() {
        try {
            if (StringUtils.isBlank(url)) {
                return;
            }

            csUrl = new URL(url);

            if (org.apache.commons.lang3.StringUtils.isBlank(getId())) {
                throw new IllegalArgumentException("Matrix Listener ID is not set");
            }

            if (StringUtils.isBlank(getLocalpart())) {
                throw new IllegalArgumentException("localpart for matrix listener is not set");
            }

            if (StringUtils.isBlank(getToken().getAs())) {
                throw new IllegalArgumentException("AS token is not set");
            }

            if (StringUtils.isBlank(getToken().getHs())) {
                throw new IllegalArgumentException("HS token is not set");
            }
        } catch (MalformedURLException e) {
            throw new ConfigurationException(e);
        }
    }

}
