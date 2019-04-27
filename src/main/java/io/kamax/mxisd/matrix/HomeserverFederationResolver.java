/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2019 Kamax Sarl
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

package io.kamax.mxisd.matrix;

import com.google.gson.JsonObject;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.matrix.json.InvalidJsonException;
import io.kamax.mxisd.dns.FederationDnsOverwrite;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class HomeserverFederationResolver {

    private static final Logger log = LoggerFactory.getLogger(HomeserverFederationResolver.class);

    private FederationDnsOverwrite dns;
    private CloseableHttpClient client;

    public HomeserverFederationResolver(FederationDnsOverwrite dns, CloseableHttpClient client) {
        this.dns = dns;
        this.client = client;
    }

    private String getDefaultScheme() {
        return "https";
    }

    private int getDefaultPort() {
        return 8448;
    }

    private String getDnsSrvPrefix() {
        return "_matrix._tcp.";
    }

    private String buildSrvRecordName(String domain) {
        return getDnsSrvPrefix() + domain;
    }

    private Optional<URL> resolveOverwrite(String domain) {
        Optional<String> entryOpt = dns.findHost(domain);
        if (!entryOpt.isPresent()) {
            log.info("No DNS overwrite for {}", domain);
            return Optional.empty();
        }

        try {
            return Optional.of(new URL(entryOpt.get()));
        } catch (MalformedURLException e) {
            log.warn("Skipping homeserver Federation DNS overwrite for {} - not a valid URL: {}", domain, entryOpt.get());
            return Optional.empty();
        }
    }

    private Optional<URL> resolveLiteral(String domain) {
        if (domain.contains("[") && domain.contains("]")) {
            // This is an IPv6
            if (domain.contains("]:")) {
                // With a custom port, we return as is
                return Optional.of(build(domain));
            } else {
                return Optional.of(build(domain + ":" + getDefaultPort()));
            }
        }

        if (domain.contains(":")) {
            // This is a domain or IPv4 with an explicit port, we return as is
            return Optional.of(build(domain));
        }

        // At this point, we do not account for the provided string to be an IPv4 without a port. We will therefore
        // perform well-known lookup and SRV record. While this is not needed, we don't expect the SRV to return anything
        // and the well-known shouldn't either, but it might, leading to a wrong destination potentially.
        //
        // We accept this risk as mxisd is not meant to be used without DNS domain as per FAQ. We also provide resolution
        // override facilities. Therefore, we accept to not handle this case until we get report of such unwanted behaviour
        // that still fix mxisd use case and can't be resolved via override.

        return Optional.empty();
    }

    private Optional<URL> resolveWellKnown(String domain) {
        log.debug("Performing Well-known lookup for {}", domain);
        HttpGet wnReq = new HttpGet("https://" + domain + "/.well-known/matrix/server");
        try (CloseableHttpResponse wnRes = client.execute(wnReq)) {
            int status = wnRes.getStatusLine().getStatusCode();
            if (status == 200) {
                try {
                    JsonObject body = GsonUtil.parseObj(EntityUtils.toString(wnRes.getEntity()));
                    String server = GsonUtil.getStringOrNull(body, "m.server");
                    if (StringUtils.isNotBlank(server)) {
                        log.debug("Found well-known entry: {}", server);
                        return Optional.of(build(server));
                    }
                } catch (InvalidJsonException e) {
                    log.info("Could not parse well-known resource: {}", e.getMessage());
                }
            } else {
                log.info("Well-known did not return status code 200 but {}, ignoring", status);
            }

            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException("Error while trying to lookup well-known for " + domain, e);
        }
    }

    private Optional<URL> resolveDnsSrv(String domain) {
        log.debug("Performing SRV lookup for {}", domain);
        String lookupDns = buildSrvRecordName(domain);
        log.debug("Lookup name: {}", lookupDns);

        try {
            List<SRVRecord> srvRecords = new ArrayList<>();
            Record[] rawRecords = new Lookup(lookupDns, Type.SRV).run();
            if (Objects.isNull(rawRecords) || rawRecords.length == 0) {
                log.debug("No SRV record for {}", domain);
                return Optional.empty();
            }

            for (Record record : rawRecords) {
                if (Type.SRV == record.getType()) {
                    srvRecords.add((SRVRecord) record);
                } else {
                    log.debug("Ignoring non-SRV record: {}", record.toString());
                }
            }

            if (srvRecords.size() < 1) {
                log.warn("DNS SRV records were found for {} but none is usable", lookupDns);
                return Optional.empty();
            }

            srvRecords.sort(Comparator.comparingInt(SRVRecord::getPriority));
            SRVRecord record = srvRecords.get(0);
            return Optional.of(build(record.getTarget().toString(true) + ":" + record.getPort()));
        } catch (TextParseException e) {
            log.warn("Unable to perform DNS SRV query for {}: {}", lookupDns, e.getMessage());
        }

        return Optional.empty();
    }

    public URL build(String authority) {
        try {
            return new URL(getDefaultScheme() + "://" + authority);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not build URL for " + authority, e);
        }
    }

    public URL resolve(String domain) {
        Optional<URL> s1 = resolveOverwrite(domain);
        if (s1.isPresent()) {
            URL dest = s1.get();
            log.info("Resolution of {} via DNS overwrite to {}", domain, dest);
            return dest;
        }

        Optional<URL> s2 = resolveLiteral(domain);
        if (s2.isPresent()) {
            URL dest = s2.get();
            log.info("Resolution of {} as IP literal or IP/hostname with explicit port to {}", domain, dest);
            return dest;
        }

        Optional<URL> s3 = resolveWellKnown(domain);
        if (s3.isPresent()) {
            URL dest = s3.get();
            log.info("Resolution of {} via well-known to {}", domain, dest);
            return dest;
        }
        // The domain needs to be resolved

        Optional<URL> s4 = resolveDnsSrv(domain);
        if (s4.isPresent()) {
            URL dest = s4.get();
            log.info("Resolution of {} via DNS SRV record to {}", domain, dest);
            return dest;
        }

        URL dest = build(domain + ":" + getDefaultPort());
        log.info("Resolution of {} to {}", domain, dest);
        return dest;
    }

}
