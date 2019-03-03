/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2019 Kamax Sarl
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

package io.kamax.mxisd.as.registration;

import io.kamax.mxisd.config.AppServiceConfig;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SynapseRegistrationYaml {

    public static SynapseRegistrationYaml parse(AppServiceConfig cfg, String domain) {
        SynapseRegistrationYaml yaml = new SynapseRegistrationYaml();

        yaml.setId(cfg.getRegistration().getSynapse().getId());
        yaml.setUrl(cfg.getEndpoint().getToAS().getUrl());
        yaml.setAsToken(cfg.getEndpoint().getToHS().getToken());
        yaml.setHsToken(cfg.getEndpoint().getToAS().getToken());
        yaml.setSenderLocalpart(cfg.getUser().getMain());

        if (cfg.getFeature().getCleanExpiredInvite()) {
            Namespace ns = new Namespace();
            ns.setExclusive(true);
            ns.setRegex("@" + cfg.getUser().getInviteExpired() + ":" + domain);
            yaml.getNamespaces().getUsers().add(ns);
        }

        if (cfg.getFeature().getInviteById()) {
            Namespace ns = new Namespace();
            ns.setExclusive(false);
            ns.setRegex("@*:" + domain);
            yaml.getNamespaces().getUsers().add(ns);
        }

        return yaml;
    }

    public static class Namespace {

        private String regex;
        private boolean exclusive;

        public String getRegex() {
            return regex;
        }

        public void setRegex(String regex) {
            this.regex = regex;
        }

        public boolean isExclusive() {
            return exclusive;
        }

        public void setExclusive(boolean exclusive) {
            this.exclusive = exclusive;
        }

    }

    public static class Namespaces {

        private List<Namespace> users = new ArrayList<>();
        private List<Namespace> aliases = new ArrayList<>();
        private List<Namespace> rooms = new ArrayList<>();

        public List<Namespace> getUsers() {
            return users;
        }

        public void setUsers(List<Namespace> users) {
            this.users = users;
        }

        public List<Namespace> getAliases() {
            return aliases;
        }

        public void setAliases(List<Namespace> aliases) {
            this.aliases = aliases;
        }

        public List<Namespace> getRooms() {
            return rooms;
        }

        public void setRooms(List<Namespace> rooms) {
            this.rooms = rooms;
        }

    }

    private String id;
    private String url;
    private String as_token;
    private String hs_token;
    private String sender_localpart;
    private Namespaces namespaces = new Namespaces();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUrl(URL url) {
        if (Objects.isNull(url)) {
            this.url = null;
        } else {
            this.url = url.toString();
        }
    }

    public String getAsToken() {
        return as_token;
    }

    public void setAsToken(String as_token) {
        this.as_token = as_token;
    }

    public String getHsToken() {
        return hs_token;
    }

    public void setHsToken(String hs_token) {
        this.hs_token = hs_token;
    }

    public String getSenderLocalpart() {
        return sender_localpart;
    }

    public void setSenderLocalpart(String sender_localpart) {
        this.sender_localpart = sender_localpart;
    }

    public Namespaces getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Namespaces namespaces) {
        this.namespaces = namespaces;
    }

}
