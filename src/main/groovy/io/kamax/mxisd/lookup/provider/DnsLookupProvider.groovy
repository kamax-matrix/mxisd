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

package io.kamax.mxisd.lookup.provider

import io.kamax.mxisd.config.MatrixConfig
import io.kamax.mxisd.lookup.SingleLookupReply
import io.kamax.mxisd.lookup.SingleLookupRequest
import io.kamax.mxisd.lookup.ThreePidMapping
import io.kamax.mxisd.lookup.fetcher.IRemoteIdentityServerFetcher
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.xbill.DNS.Lookup
import org.xbill.DNS.SRVRecord
import org.xbill.DNS.Type

import java.util.concurrent.ForkJoinPool
import java.util.concurrent.RecursiveTask
import java.util.function.Function

@Component
class DnsLookupProvider implements IThreePidProvider {

    private Logger log = LoggerFactory.getLogger(DnsLookupProvider.class)

    @Autowired
    private MatrixConfig mxCfg

    @Autowired
    private IRemoteIdentityServerFetcher fetcher

    @Override
    boolean isEnabled() {
        return true
    }

    @Override
    boolean isLocal() {
        return false
    }

    @Override
    int getPriority() {
        return 10
    }

    String getSrvRecordName(String domain) {
        return "_matrix-identity._tcp." + domain
    }

    Optional<String> getDomain(String email) {
        int atIndex = email.lastIndexOf("@")
        if (atIndex == -1) {
            return Optional.empty()
        }

        return Optional.of(email.substring(atIndex + 1))
    }

    // TODO use caching mechanism
    Optional<String> findIdentityServerForDomain(String domain) {
        if (StringUtils.equals(mxCfg.getDomain(), domain)) {
            log.info("We are authoritative for {}, no remote lookup", domain)
            return Optional.empty()
        }

        log.info("Performing SRV lookup")
        String lookupDns = getSrvRecordName(domain)
        log.info("Lookup name: {}", lookupDns)

        SRVRecord[] records = (SRVRecord[]) new Lookup(lookupDns, Type.SRV).run()
        if (records != null) {
            Arrays.sort(records, new Comparator<SRVRecord>() {

                @Override
                int compare(SRVRecord o1, SRVRecord o2) {
                    return Integer.compare(o1.getPriority(), o2.getPriority())
                }

            })

            for (SRVRecord record : records) {
                log.info("Found SRV record: {}", record.toString())
                String baseUrl = "https://${record.getTarget().toString(true)}:${record.getPort()}"
                if (fetcher.isUsable(baseUrl)) {
                    log.info("Found Identity Server for domain {} at {}", domain, baseUrl)
                    return Optional.of(baseUrl)
                } else {
                    log.info("{} is not a usable Identity Server", baseUrl)
                }
            }
        } else {
            log.info("No SRV record for {}", lookupDns)
        }

        log.info("Performing basic lookup using domain name {}", domain)
        String baseUrl = "https://" + domain
        if (fetcher.isUsable(baseUrl)) {
            log.info("Found Identity Server for domain {} at {}", domain, baseUrl)
            return Optional.of(baseUrl)
        } else {
            log.info("{} is not a usable Identity Server", baseUrl)
            return Optional.empty()
        }
    }

    @Override
    Optional<SingleLookupReply> find(SingleLookupRequest request) {
        if (!StringUtils.equals("email", request.getType())) { // TODO use enum
            log.info("Skipping unsupported type {} for {}", request.getType(), request.getThreePid())
            return Optional.empty()
        }

        log.info("Performing DNS lookup for {}", request.getThreePid())

        String domain = request.getThreePid().substring(request.getThreePid().lastIndexOf("@") + 1)
        log.info("Domain name for {}: {}", request.getThreePid(), domain)
        Optional<String> baseUrl = findIdentityServerForDomain(domain)

        if (baseUrl.isPresent()) {
            return fetcher.find(baseUrl.get(), request)
        }

        return Optional.empty()
    }

    @Override
    List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        Map<String, List<ThreePidMapping>> domains = new HashMap<>()

        for (ThreePidMapping mapping : mappings) {
            if (!StringUtils.equals("email", mapping.getMedium())) {
                log.info("Skipping unsupported type {} for {}", mapping.getMedium(), mapping.getValue())
                continue
            }

            Optional<String> domainOpt = getDomain(mapping.getValue())
            if (!domainOpt.isPresent()) {
                log.warn("No domain for 3PID {}", mapping.getValue())
                continue
            }

            String domain = domainOpt.get()
            List<ThreePidMapping> domainMappings = domains.computeIfAbsent(domain, new Function<String, List<ThreePidMapping>>() {

                @Override
                List<ThreePidMapping> apply(String s) {
                    return new ArrayList<>()
                }

            })
            domainMappings.add(mapping)
        }

        log.info("Looking mappings across {} domains", domains.keySet().size())
        ForkJoinPool pool = new ForkJoinPool()
        RecursiveTask<List<ThreePidMapping>> task = new RecursiveTask<List<ThreePidMapping>>() {

            @Override
            protected List<ThreePidMapping> compute() {
                List<ThreePidMapping> mappingsFound = new ArrayList<>()
                List<DomainBulkLookupTask> tasks = new ArrayList<>()

                for (String domain : domains.keySet()) {
                    DomainBulkLookupTask domainTask = new DomainBulkLookupTask(domain, domains.get(domain))
                    domainTask.fork()
                    tasks.add(domainTask)
                }

                for (DomainBulkLookupTask task : tasks) {
                    mappingsFound.addAll(task.join())
                }

                return mappingsFound
            }
        }
        pool.submit(task)
        pool.shutdown()

        List<ThreePidMapping> mappingsFound = task.join()
        log.info("Found {} mappings overall", mappingsFound.size())
        return mappingsFound
    }

    private class DomainBulkLookupTask extends RecursiveTask<List<ThreePidMapping>> {

        private String domain
        private List<ThreePidMapping> mappings

        DomainBulkLookupTask(String domain, List<ThreePidMapping> mappings) {
            this.domain = domain
            this.mappings = mappings
        }

        @Override
        protected List<ThreePidMapping> compute() {
            List<ThreePidMapping> domainMappings = new ArrayList<>()

            Optional<String> baseUrl = findIdentityServerForDomain(domain)
            if (!baseUrl.isPresent()) {
                log.info("No usable Identity server for domain {}", domain)
            } else {
                domainMappings.addAll(fetcher.find(baseUrl.get(), mappings))
                log.info("Found {} mappings in domain {}", domainMappings.size(), domain)
            }

            return domainMappings
        }
    }

}
