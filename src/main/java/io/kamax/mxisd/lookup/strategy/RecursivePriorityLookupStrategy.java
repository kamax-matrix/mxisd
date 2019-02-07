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

package io.kamax.mxisd.lookup.strategy;

import edazdarevic.commons.net.CIDRUtils;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.matrix.json.MatrixJson;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.exception.ConfigurationException;
import io.kamax.mxisd.lookup.*;
import io.kamax.mxisd.lookup.fetcher.IBridgeFetcher;
import io.kamax.mxisd.lookup.provider.IThreePidProvider;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RecursivePriorityLookupStrategy implements LookupStrategy {

    private transient final Logger log = LoggerFactory.getLogger(RecursivePriorityLookupStrategy.class);

    private MxisdConfig.Lookup cfg;
    private List<IThreePidProvider> providers;
    private IBridgeFetcher bridge;

    private Map<String, CompletableFuture<List<ThreePidMapping>>> bulkLookupInProgress = new ConcurrentHashMap<>();

    private List<CIDRUtils> allowedCidr = new ArrayList<>();

    public RecursivePriorityLookupStrategy(MxisdConfig.Lookup cfg, List<? extends IThreePidProvider> providers, IBridgeFetcher bridge) {
        this.cfg = cfg;
        this.bridge = bridge;
        this.providers = new ArrayList<>(providers);

        try {
            log.info("Found {} providers", providers.size());
            providers.forEach(p -> log.info("  - {}", p.getClass().getName()));
            providers.sort((o1, o2) -> Integer.compare(o2.getPriority(), o1.getPriority()));

            log.info("Recursive lookup enabled: {}", cfg.getRecursive().isEnabled());
            for (String cidr : cfg.getRecursive().getAllowedCidr()) {
                log.info("{} is allowed for recursion", cidr);
                allowedCidr.add(new CIDRUtils(cidr));
            }
        } catch (UnknownHostException e) {
            throw new ConfigurationException("lookup.recursive.allowedCidrs", "Allowed CIDRs");
        }
    }

    private boolean isAllowedForRecursive(String source) {
        boolean canRecurse = false;

        try {
            if (cfg.getRecursive().isEnabled()) {
                log.debug("Checking {} CIDRs for recursion", allowedCidr.size());
                for (CIDRUtils cidr : allowedCidr) {
                    if (cidr.isInRange(source)) {
                        log.debug("{} is in range {}, allowing recursion", source, cidr.getNetworkAddress());
                        canRecurse = true;
                        break;
                    } else {
                        log.debug("{} is not in range {}", source, cidr.getNetworkAddress());
                    }
                }
            }
        } catch (UnknownHostException e) {
            // this should never happened as we should have only IP ranges!
            log.error("Unexpected {} exception: {}", e.getClass().getSimpleName(), e.getMessage());
        }

        return canRecurse;
    }

    private List<IThreePidProvider> listUsableProviders(ALookupRequest request) {
        return listUsableProviders(request, false);
    }

    private List<IThreePidProvider> listUsableProviders(ALookupRequest request, boolean forceRecursive) {
        List<IThreePidProvider> usableProviders = new ArrayList<>();

        boolean canRecurse = forceRecursive || isAllowedForRecursive(request.getRequester());

        log.info("Host {} allowed for recursion: {}", request.getRequester(), canRecurse);
        for (IThreePidProvider provider : providers) {
            if (provider.isLocal() || canRecurse || forceRecursive) {
                usableProviders.add(provider);
            }
        }

        return usableProviders;
    }

    @Override
    public List<IThreePidProvider> getLocalProviders() {
        return providers.stream().filter(IThreePidProvider::isLocal).collect(Collectors.toList());
    }

    public List<IThreePidProvider> getRemoteProviders() {
        return providers.stream().filter(iThreePidProvider -> !iThreePidProvider.isLocal()).collect(Collectors.toList());
    }

    private static SingleLookupRequest build(String medium, String address) {
        SingleLookupRequest req = new SingleLookupRequest();
        req.setType(medium);
        req.setThreePid(address);
        req.setRequester("Internal");
        return req;
    }

    @Override
    public Optional<SingleLookupReply> find(String medium, String address, boolean recursive) {
        return find(build(medium, address), recursive);
    }

    @Override
    public Optional<SingleLookupReply> findLocal(String medium, String address) {
        return find(build(medium, address), getLocalProviders());
    }

    @Override
    public Optional<SingleLookupReply> findRemote(String medium, String address) {
        return find(build(medium, address), getRemoteProviders());
    }

    public Optional<SingleLookupReply> find(SingleLookupRequest request, boolean forceRecursive) {
        return find(request, listUsableProviders(request, forceRecursive));
    }

    public Optional<SingleLookupReply> find(SingleLookupRequest request, List<IThreePidProvider> providers) {
        for (IThreePidProvider provider : providers) {
            Optional<SingleLookupReply> lookupDataOpt = provider.find(request);
            if (lookupDataOpt.isPresent()) {
                log.info("Found 3PID mapping: {medium: '{}', address: '{}', mxid: '{}'}",
                        request.getType(), request.getThreePid(), lookupDataOpt.get().getMxid().getId());
                return lookupDataOpt;
            }
        }

        if (
                cfg.getRecursive().getBridge() != null &&
                        cfg.getRecursive().getBridge().getEnabled() &&
                        (!cfg.getRecursive().getBridge().getRecursiveOnly() || isAllowedForRecursive(request.getRequester()))
        ) {
            log.info("Using bridge failover for lookup");
            Optional<SingleLookupReply> lookupDataOpt = bridge.find(request);
            log.info("Found 3PID mapping: {medium: '{}', address: '{}', mxid: '{}'}",
                    request.getThreePid(), request.getId(), lookupDataOpt.get().getMxid().getId());
            return lookupDataOpt;
        }

        log.info("No 3PID mapping found");
        return Optional.empty();
    }

    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        return find(request, false);
    }

    @Override
    public Optional<SingleLookupReply> findRecursive(SingleLookupRequest request) {
        return find(request, true);
    }

    @Override
    public CompletableFuture<List<ThreePidMapping>> find(BulkLookupRequest request) {
        if (!cfg.getBulk().getEnabled()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        String payloadId = DigestUtils.md5Hex(MatrixJson.encodeCanonical(GsonUtil.makeObj(request)));

        log.info("Computed Payload ID: {}", payloadId);
        synchronized (this) {
            CompletableFuture<List<ThreePidMapping>> f = bulkLookupInProgress.get(payloadId);
            if (Objects.nonNull(f)) {
                log.info("Returning existing future for Payload ID {}", payloadId);
                return f;
            }

            bulkLookupInProgress.put(payloadId, new CompletableFuture<>());
        }

        log.info("Processing Payload ID {}", payloadId);

        CompletableFuture<List<ThreePidMapping>> result = bulkLookupInProgress.get(payloadId);
        List<ThreePidMapping> mapToDo = new ArrayList<>(request.getMappings());
        List<ThreePidMapping> mapFoundAll = new ArrayList<>();

        for (IThreePidProvider provider : listUsableProviders(request)) {
            if (mapToDo.isEmpty()) {
                log.info("No more mappings to lookup");
                break;
            } else {
                log.info("{} mappings remaining overall", mapToDo.size());
            }

            log.info("Using provider {} for remaining mappings", provider.getClass().getSimpleName());
            List<ThreePidMapping> mapFound = provider.populate(mapToDo);
            log.info("Provider {} returned {} mappings", provider.getClass().getSimpleName(), mapFound.size());
            mapFoundAll.addAll(mapFound);
            mapToDo.removeAll(mapFound);
        }

        log.info("Processed Payload ID {}", payloadId);
        result.complete(mapFoundAll);
        return bulkLookupInProgress.remove(payloadId);
    }

}
