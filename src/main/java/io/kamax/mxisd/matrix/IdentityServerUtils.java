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

package io.kamax.mxisd.matrix;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.kamax.mxisd.http.IsAPIv1;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// FIXME placeholder, this must go in matrix-java-sdk for 1.0
// FIXME this class is just a mistake and should never have happened. Make sure to get rid of for v2.x
public class IdentityServerUtils {

    private static Logger log = LoggerFactory.getLogger(IdentityServerUtils.class);
    private static JsonParser parser = new JsonParser();

    private static CloseableHttpClient client;

    public static void setHttpClient(CloseableHttpClient client) {
        IdentityServerUtils.client = client;
    }

    public static boolean isUsable(String remote) {
        if (StringUtils.isBlank(remote)) {
            log.info("IS URL is blank, not usable");
            return false;
        }

        HttpGet req = new HttpGet(URI.create(remote + IsAPIv1.Base));
        req.setConfig(RequestConfig.custom()
                .setConnectTimeout(2000)
                .setConnectionRequestTimeout(2000)
                .build()
        );

        try (CloseableHttpResponse res = client.execute(req)) {
            int status = res.getStatusLine().getStatusCode();
            if (status != 200) {
                log.info("Usability of {} as Identity server: answer status: {}", remote, status);
                return false;
            }

            JsonElement el = parser.parse(IOUtils.toString(res.getEntity().getContent(), StandardCharsets.UTF_8));
            if (!el.isJsonObject()) {
                log.debug("IS {} did not send back an empty JSON object as per spec, not a valid IS", remote);
                return false;
            }

            return true;
        } catch (IllegalArgumentException | IOException | JsonParseException e) {
            log.info("{} is not a usable Identity Server: {}", remote, e.getMessage());
            return false;
        }
    }

    private static String getSrvRecordName(String domain) {
        return "_matrix-identity._tcp." + domain;
    }

    public static Optional<String> findIsUrlForDomain(String domainOrUrl) {
        try {
            try {
                domainOrUrl = new URL(domainOrUrl).getHost();
            } catch (MalformedURLException e) {
                log.info("{} is not an URL, using as-is", domainOrUrl);
            }

            log.info("Discovering Identity Server for {}", domainOrUrl);
            log.info("Performing SRV lookup");
            String lookupDns = getSrvRecordName(domainOrUrl);
            log.info("Lookup name: {}", lookupDns);

            List<SRVRecord> srvRecords = new ArrayList<>();
            Record[] records = new Lookup(lookupDns, Type.SRV).run();
            if (records == null || records.length == 0) {
                log.info("No SRV record for {}", lookupDns);
                return Optional.empty();
            }

            for (Record record : records) {
                log.info("Record: {}", record.toString());
                if (record.getType() == Type.SRV) {
                    if (record instanceof SRVRecord) {
                        srvRecords.add((SRVRecord) record);
                    } else {
                        log.warn("We requested SRV records but we got {} instead!", record.getClass().getName());
                    }
                } else {
                    log.warn("We request SRV type records but we got type #{} instead!", record.getType());
                }
            }
            srvRecords.sort(Comparator.comparingInt(SRVRecord::getPriority));

            for (SRVRecord srvRecord : srvRecords) {
                String baseUrl = "https://" + srvRecord.getTarget().toString(true) + ":" + srvRecord.getPort();
                if (isUsable(baseUrl)) {
                    log.info("Found Identity Server for domain {} at {}", domainOrUrl, baseUrl);
                    return Optional.of(baseUrl);
                } else {
                    log.info("Found no Identity server for domain {} at {}", domainOrUrl, baseUrl);
                }
            }

            log.info("Found no Identity server for domain {}", domainOrUrl);
            return Optional.empty();
        } catch (TextParseException e) {
            log.warn(domainOrUrl + " is not a valid domain name");
            return Optional.empty();
        }
    }

}
