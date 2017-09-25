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

package io.kamax.mxisd.lookup.provider;

import io.kamax.mxisd.config.ForwardConfig;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.fetcher.IRemoteIdentityServerFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
class ForwarderProvider implements IThreePidProvider {

    private Logger log = LoggerFactory.getLogger(ForwarderProvider.class);

    @Autowired
    private ForwardConfig cfg;

    @Autowired
    private IRemoteIdentityServerFetcher fetcher;

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        for (String root : cfg.getServers()) {
            Optional<SingleLookupReply> answer = fetcher.find(root, request);
            if (answer.isPresent()) {
                return answer;
            }
        }

        return Optional.empty();
    }

    @Override
    public List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        List<ThreePidMapping> mappingsToDo = new ArrayList<>(mappings);
        List<ThreePidMapping> mappingsFoundGlobal = new ArrayList<>();

        for (String root : cfg.getServers()) {
            log.info("{} mappings remaining: {}", mappingsToDo.size(), mappingsToDo);
            log.info("Querying {}", root);
            List<ThreePidMapping> mappingsFound = fetcher.find(root, mappingsToDo);
            log.info("{} returned {} mappings", root, mappingsFound.size());
            mappingsFoundGlobal.addAll(mappingsFound);
            mappingsToDo.removeAll(mappingsFound);
        }

        return mappingsFoundGlobal;
    }

}
