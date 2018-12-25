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

package io.kamax.mxisd.dns;

import io.kamax.mxisd.config.DnsOverwriteConfig;
import io.kamax.mxisd.exception.ConfigurationException;
import io.kamax.mxisd.exception.InternalServerError;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static io.kamax.mxisd.config.DnsOverwriteConfig.Entry;

public class ClientDnsOverwrite {

    private transient final Logger log = LoggerFactory.getLogger(ClientDnsOverwrite.class);

    private Map<String, Entry> mappings;

    public ClientDnsOverwrite(DnsOverwriteConfig cfg) {
        mappings = new HashMap<>();
        cfg.getHomeserver().getClient().forEach(e -> mappings.put(e.getName(), e));
    }

    public URIBuilder transform(URI initial) {
        URIBuilder builder = new URIBuilder(initial);
        Entry mapping = mappings.get(initial.getHost());
        if (mapping == null) {
            throw new InternalServerError("No DNS client override for " + initial.getHost());
        }

        try {
            URL target = new URL(mapping.getValue());
            builder.setScheme(target.getProtocol());
            builder.setHost(target.getHost());
            if (target.getPort() != -1) {
                builder.setPort(target.getPort());
            }

            return builder;
        } catch (MalformedURLException e) {
            log.warn("Skipping DNS overwrite entry {} due to invalid value [{}]: {}", mapping.getName(), mapping.getValue(), e.getMessage());
            throw new ConfigurationException("Invalid DNS overwrite entry in homeserver client: " + mapping.getName(), e.getMessage());
        }
    }

}
