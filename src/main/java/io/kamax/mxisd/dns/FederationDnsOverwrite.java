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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.kamax.mxisd.config.DnsOverwriteConfig.Entry;

public class FederationDnsOverwrite {

    private transient final Logger log = LoggerFactory.getLogger(FederationDnsOverwrite.class);

    private Map<String, Entry> mappings;

    public FederationDnsOverwrite(DnsOverwriteConfig cfg) {

        mappings = new HashMap<>();
        cfg.getHomeserver().getFederation().forEach(e -> mappings.put(e.getName(), e));
    }

    public Optional<String> findHost(String lookup) {
        Entry mapping = mappings.get(lookup);
        if (mapping == null) {
            return Optional.empty();
        }

        return Optional.of(mapping.getValue());
    }

}
