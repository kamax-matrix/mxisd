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

package io.kamax.mxisd.lookup.provider;

import io.kamax.mxisd.config.RecursiveLookupBridgeConfig;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.fetcher.IBridgeFetcher;
import io.kamax.mxisd.lookup.fetcher.IRemoteIdentityServerFetcher;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BridgeFetcher implements IBridgeFetcher {

    private transient final Logger log = LoggerFactory.getLogger(BridgeFetcher.class);

    private RecursiveLookupBridgeConfig cfg;
    private IRemoteIdentityServerFetcher fetcher;

    public BridgeFetcher(RecursiveLookupBridgeConfig cfg, IRemoteIdentityServerFetcher fetcher) {
        this.cfg = cfg;
        this.fetcher = fetcher;
    }

    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        Optional<String> mediumUrl = Optional.ofNullable(cfg.getMappings().get(request.getType()));
        if (mediumUrl.isPresent() && !StringUtils.isBlank(mediumUrl.get())) {
            log.info("Using specific medium bridge lookup URL {}", mediumUrl.get());

            return fetcher.find(mediumUrl.get(), request);
        } else if (!StringUtils.isBlank(cfg.getServer())) {
            log.info("Using generic bridge lookup URL {}", cfg.getServer());

            return fetcher.find(cfg.getServer(), request);
        } else {
            log.info("No bridge lookup URL found/configured, skipping");

            return Optional.empty();
        }
    }

    @Override
    public List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        log.warn("Bulk lookup on bridge lookup requested, but not supported - returning empty list");

        return Collections.emptyList();
    }

}
