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

import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.fetcher.IRemoteIdentityServerFetcher;
import io.kamax.mxisd.matrix.IdentityServerUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

class DnsLookupProvider implements IThreePidProvider {

    private transient final Logger log = LoggerFactory.getLogger(DnsLookupProvider.class);

    private MatrixConfig cfg;
    private IRemoteIdentityServerFetcher fetcher;

    public DnsLookupProvider(MatrixConfig cfg, IRemoteIdentityServerFetcher fetcher) {
        this.cfg = cfg;
        this.fetcher = fetcher;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    private Optional<String> getDomain(String email) {
        int atIndex = email.lastIndexOf("@");
        if (atIndex == -1) {
            return Optional.empty();
        }

        return Optional.of(email.substring(atIndex + 1));
    }

    // TODO use caching mechanism
    private Optional<String> findIdentityServerForDomain(String domain) {
        if (StringUtils.equals(cfg.getDomain(), domain)) {
            log.info("We are authoritative for {}, no remote lookup", domain);
            return Optional.empty();
        }

        return IdentityServerUtils.findIsUrlForDomain(domain);
    }

    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        if (!ThreePidMedium.Email.is(request.getType())) { // TODO use enum
            log.info("Skipping unsupported type {} for {}", request.getType(), request.getThreePid());
            return Optional.empty();
        }

        log.info("Performing DNS lookup for {}", request.getThreePid());

        String domain = request.getThreePid().substring(request.getThreePid().lastIndexOf("@") + 1);
        log.info("Domain name for {}: {}", request.getThreePid(), domain);
        Optional<String> baseUrl = findIdentityServerForDomain(domain);

        if (baseUrl.isPresent()) {
            return fetcher.find(baseUrl.get(), request);
        }

        return Optional.empty();
    }

    @Override
    public List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        Map<String, List<ThreePidMapping>> domains = new HashMap<>();

        for (ThreePidMapping mapping : mappings) {
            if (!ThreePidMedium.Email.is(mapping.getMedium())) {
                log.info("Skipping unsupported type {} for {}", mapping.getMedium(), mapping.getValue());
                continue;
            }

            Optional<String> domainOpt = getDomain(mapping.getValue());
            if (!domainOpt.isPresent()) {
                log.warn("No domain for 3PID {}", mapping.getValue());
                continue;
            }

            String domain = domainOpt.get();
            List<ThreePidMapping> domainMappings = domains.computeIfAbsent(domain, s -> new ArrayList<>());
            domainMappings.add(mapping);
        }

        log.info("Looking mappings across {} domains", domains.keySet().size());
        ForkJoinPool pool = ForkJoinPool.commonPool();
        RecursiveTask<List<ThreePidMapping>> task = new RecursiveTask<List<ThreePidMapping>>() {

            @Override
            protected List<ThreePidMapping> compute() {
                List<ThreePidMapping> mappingsFound = new ArrayList<>();
                List<DomainBulkLookupTask> tasks = new ArrayList<>();

                for (String domain : domains.keySet()) {
                    DomainBulkLookupTask domainTask = new DomainBulkLookupTask(domain, domains.get(domain));
                    domainTask.fork();
                    tasks.add(domainTask);
                }

                for (DomainBulkLookupTask task : tasks) {
                    mappingsFound.addAll(task.join());
                }

                return mappingsFound;
            }
        };
        pool.submit(task);
        pool.shutdown();

        List<ThreePidMapping> mappingsFound = task.join();
        log.info("Found {} mappings overall", mappingsFound.size());
        return mappingsFound;
    }

    private class DomainBulkLookupTask extends RecursiveTask<List<ThreePidMapping>> {

        private String domain;
        private List<ThreePidMapping> mappings;

        DomainBulkLookupTask(String domain, List<ThreePidMapping> mappings) {
            this.domain = domain;
            this.mappings = mappings;
        }

        @Override
        protected List<ThreePidMapping> compute() {
            List<ThreePidMapping> domainMappings = new ArrayList<>();

            Optional<String> baseUrl = findIdentityServerForDomain(domain);
            if (!baseUrl.isPresent()) {
                log.info("No usable Identity server for domain {}", domain);
            } else {
                domainMappings.addAll(fetcher.find(baseUrl.get(), mappings));
                log.info("Found {} mappings in domain {}", domainMappings.size(), domain);
            }

            return domainMappings;
        }
    }

}
