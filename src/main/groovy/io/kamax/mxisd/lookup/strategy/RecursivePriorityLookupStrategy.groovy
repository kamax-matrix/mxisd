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

package io.kamax.mxisd.lookup.strategy

import edazdarevic.commons.net.CIDRUtils
import io.kamax.mxisd.config.RecursiveLookupConfig
import io.kamax.mxisd.lookup.ALookupRequest
import io.kamax.mxisd.lookup.BulkLookupRequest
import io.kamax.mxisd.lookup.SingleLookupRequest
import io.kamax.mxisd.lookup.ThreePidMapping
import io.kamax.mxisd.lookup.fetcher.IBridgeFetcher
import io.kamax.mxisd.lookup.provider.IThreePidProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RecursivePriorityLookupStrategy implements LookupStrategy, InitializingBean {

    private Logger log = LoggerFactory.getLogger(RecursivePriorityLookupStrategy.class)

    @Autowired
    private RecursiveLookupConfig recursiveCfg

    @Autowired
    private List<IThreePidProvider> providers

    @Autowired
    private IBridgeFetcher bridge

    private List<CIDRUtils> allowedCidr = new ArrayList<>()

    @Override
    void afterPropertiesSet() throws Exception {
        log.info("Found ${providers.size()} providers")

        providers.sort(new Comparator<IThreePidProvider>() {

            @Override
            int compare(IThreePidProvider o1, IThreePidProvider o2) {
                return Integer.compare(o2.getPriority(), o1.getPriority())
            }

        })

        log.info("Recursive lookup enabled: {}", recursiveCfg.isEnabled())
        for (String cidr : recursiveCfg.getAllowedCidr()) {
            log.info("{} is allowed for recursion", cidr)
            allowedCidr.add(new CIDRUtils(cidr))
        }
    }

    boolean isAllowedForRecursive(String source) {
        boolean canRecurse = false

        if (recursiveCfg.isEnabled()) {
            log.debug("Checking {} CIDRs for recursion", allowedCidr.size())
            for (CIDRUtils cidr : allowedCidr) {
                if (cidr.isInRange(source)) {
                    log.debug("{} is in range {}, allowing recursion", source, cidr.getNetworkAddress())
                    canRecurse = true
                    break
                } else {
                    log.debug("{} is not in range {}", source, cidr.getNetworkAddress())
                }
            }
        }

        return canRecurse
    }

    List<IThreePidProvider> listUsableProviders(ALookupRequest request) {
        List<IThreePidProvider> usableProviders = new ArrayList<>()

        boolean canRecurse = isAllowedForRecursive(request.getRequester())

        log.info("Host {} allowed for recursion: {}", request.getRequester(), canRecurse)
        for (IThreePidProvider provider : providers) {
            if (provider.isLocal() || canRecurse) {
                usableProviders.add(provider)
            }
        }

        return usableProviders
    }

    @Override
    Optional<?> find(SingleLookupRequest request) {
        for (IThreePidProvider provider : listUsableProviders(request)) {
            Optional<?> lookupDataOpt = provider.find(request)
            if (lookupDataOpt.isPresent()) {
                return lookupDataOpt
            }
        }

        if (recursiveCfg.getBridge().getEnabled() && (!recursiveCfg.getBridge().getRecursiveOnly() || isAllowedForRecursive(request.getRequester()))) {
            log.info("Using bridge failover for lookup")
            return bridge.find(request)
        }

        return Optional.empty()
    }

    @Override
    List<ThreePidMapping> find(BulkLookupRequest request) {
        List<ThreePidMapping> mapToDo = new ArrayList<>(request.getMappings())
        List<ThreePidMapping> mapFoundAll = new ArrayList<>()

        for (IThreePidProvider provider : listUsableProviders(request)) {
            if (mapToDo.isEmpty()) {
                log.info("No more mappings to lookup")
                break
            } else {
                log.info("{} mappings remaining overall", mapToDo.size())
            }

            log.info("Using provider {} for remaining mappings", provider.getClass().getSimpleName())
            List<ThreePidMapping> mapFound = provider.populate(mapToDo)
            log.info("Provider {} returned {} mappings", provider.getClass().getSimpleName(), mapFound.size())
            mapFoundAll.addAll(mapFound)
            mapToDo.removeAll(mapFound)
        }

        return mapFoundAll
    }

}
