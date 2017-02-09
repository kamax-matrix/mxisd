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

package io.kamax.mxisd.lookup

import io.kamax.mxisd.api.ThreePidType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.xbill.DNS.Lookup
import org.xbill.DNS.SRVRecord
import org.xbill.DNS.Type

@Component
class DnsLookupProvider extends RemoteIdentityServerProvider {

    private Logger log = LoggerFactory.getLogger(DnsLookupProvider.class)

    @Override
    int getPriority() {
        return 10
    }

    @Override
    Optional<?> find(ThreePidType type, String threePid) {
        log.info("Performing DNS lookup for {}", threePid)
        if (ThreePidType.email != type) {
            log.info("Skipping unsupported type {} for {}", type, threePid)
            return Optional.empty()
        }

        String domain = threePid.substring(threePid.lastIndexOf("@") + 1)
        log.info("Domain name for {}: {}", threePid, domain)

        log.info("Performing SRV lookup")
        String lookupDns = "_matrix-identity._tcp." + domain
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
                Optional<?> answer = find(baseUrl, type, threePid)
                if (answer.isPresent()) {
                    return answer
                } else {
                    log.info("No mapping found at {}", baseUrl)
                }
            }
        } else {
            log.info("No SRV record for {}", lookupDns)
        }

        log.info("Performing basic lookup using domain name {}", domain)
        String baseUrl = "https://" + domain
        return find(baseUrl, type, threePid)
    }

}
