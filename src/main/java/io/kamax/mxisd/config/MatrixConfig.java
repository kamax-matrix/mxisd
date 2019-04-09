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

import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.exception.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatrixConfig {

    public static class Identity {

        private Map<String, List<String>> servers = new HashMap<>();

        public Identity() {
            servers.put("matrix-org", Collections.singletonList("https://matrix.org"));
        }

        public Map<String, List<String>> getServers() {
            return servers;
        }

        public void setServers(Map<String, List<String>> servers) {
            this.servers = servers;
        }

        public List<String> getServers(String label) {
            if (!servers.containsKey(label)) {
                throw new RuntimeException("No Identity server list with label '" + label + "'");
            }

            return servers.get(label);
        }

    }

    private transient final Logger log = LoggerFactory.getLogger(MatrixConfig.class);

    private String domain;
    private Identity identity = new Identity();

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public void build() {
        log.info("--- Matrix config ---");

        if (StringUtils.isBlank(domain)) {
            throw new ConfigurationException("matrix.domain");
        }

        log.info("Domain: {}", getDomain());
        log.info("Identity:");
        log.info("\tServers: {}", GsonUtil.get().toJson(identity.getServers()));
    }

}
