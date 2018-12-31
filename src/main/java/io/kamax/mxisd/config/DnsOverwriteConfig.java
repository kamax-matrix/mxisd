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

package io.kamax.mxisd.config;

import com.google.gson.Gson;
import io.kamax.mxisd.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DnsOverwriteConfig {

    private transient final Logger log = LoggerFactory.getLogger(DnsOverwriteConfig.class);

    public static class Entry {

        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

    public static class Type {

        List<Entry> client = new ArrayList<>();
        List<Entry> federation = new ArrayList<>();

        public List<Entry> getClient() {
            return client;
        }

        public void setClient(List<Entry> client) {
            this.client = client;
        }

        public List<Entry> getFederation() {
            return federation;
        }

        public void setFederation(List<Entry> federation) {
            this.federation = federation;
        }

    }

    private Type homeserver = new Type();

    public Type getHomeserver() {
        return homeserver;
    }

    public void setHomeserver(Type homeserver) {
        this.homeserver = homeserver;
    }

    public void build() {
        Gson gson = GsonUtil.build();
        log.info("--- DNS Overwrite config ---");
        log.info("Homeserver:");
        log.info("\tClient: {}", gson.toJson(getHomeserver().getClient()));
        log.info("\tFederation: {}", gson.toJson(getHomeserver().getFederation()));

    }

}
