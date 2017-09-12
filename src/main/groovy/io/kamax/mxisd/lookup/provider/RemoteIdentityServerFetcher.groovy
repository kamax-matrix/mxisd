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

import groovy.json.JsonException
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.kamax.mxisd.controller.v1.ClientBulkLookupRequest
import io.kamax.mxisd.lookup.SingleLookupReply
import io.kamax.mxisd.lookup.SingleLookupRequest
import io.kamax.mxisd.lookup.ThreePidMapping
import io.kamax.mxisd.lookup.fetcher.IRemoteIdentityServerFetcher
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.HttpClients
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope("prototype")
@Lazy
public class RemoteIdentityServerFetcher implements IRemoteIdentityServerFetcher {

    public static final String THREEPID_TEST_MEDIUM = "email"
    public static final String THREEPID_TEST_ADDRESS = "john.doe@example.org"

    private Logger log = LoggerFactory.getLogger(RemoteIdentityServerFetcher.class)

    private JsonSlurper json = new JsonSlurper()

    @Override
    boolean isUsable(String remote) {
        try {
            HttpURLConnection rootSrvConn = (HttpURLConnection) new URL(
                    "${remote}/_matrix/identity/api/v1/lookup?medium=${THREEPID_TEST_MEDIUM}&address=${THREEPID_TEST_ADDRESS}"
            ).openConnection()
            // TODO turn this into a configuration property
            rootSrvConn.setConnectTimeout(2000)

            if (rootSrvConn.getResponseCode() != 200) {
                return false
            }

            def output = json.parseText(rootSrvConn.getInputStream().getText())
            if (output['address']) {
                return false
            }

            return true
        } catch (IOException | JsonException e) {
            log.info("{} is not a usable Identity Server: {}", remote, e.getMessage())
            return false
        }
    }

    @Override
    Optional<SingleLookupReply> find(String remote, SingleLookupRequest request) {
        log.info("Looking up {} 3PID {} using {}", request.getType(), request.getThreePid(), remote)

        HttpURLConnection rootSrvConn = (HttpURLConnection) new URL(
                "${remote}/_matrix/identity/api/v1/lookup?medium=${request.getType()}&address=${request.getThreePid()}"
        ).openConnection()

        try {
            String outputRaw = rootSrvConn.getInputStream().getText()
            def output = json.parseText(outputRaw)
            if (output['address']) {
                log.info("Found 3PID mapping: {}", output)

                return Optional.of(SingleLookupReply.fromRecursive(request, outputRaw))
            }

            log.info("Empty 3PID mapping from {}", remote)
            return Optional.empty()
        } catch (IOException e) {
            log.warn("Error looking up 3PID mapping {}: {}", request.getThreePid(), e.getMessage())
            return Optional.empty()
        } catch (JsonException e) {
            log.warn("Invalid JSON answer from {}", remote)
            return Optional.empty()
        }
    }

    @Override
    List<ThreePidMapping> find(String remote, List<ThreePidMapping> mappings) {
        List<ThreePidMapping> mappingsFound = new ArrayList<>()

        ClientBulkLookupRequest mappingRequest = new ClientBulkLookupRequest()
        mappingRequest.setMappings(mappings)

        String url = "${remote}/_matrix/identity/api/v1/bulk_lookup"
        HttpClient client = HttpClients.createDefault()
        try {
            HttpPost request = new HttpPost(url)
            request.setEntity(
                    EntityBuilder.create()
                            .setText(JsonOutput.toJson(mappingRequest))
                            .setContentType(ContentType.APPLICATION_JSON)
                            .build()
            )

            HttpResponse response = client.execute(request)
            try {
                if (response.getStatusLine().getStatusCode() != 200) {
                    log.info("Could not perform lookup at {} due to HTTP return code: {}", url, response.getStatusLine().getStatusCode())
                    return mappingsFound
                }

                HttpEntity entity = response.getEntity()
                if (entity != null) {
                    ClientBulkLookupRequest input = (ClientBulkLookupRequest) json.parseText(entity.getContent().getText())
                    for (List<String> mappingRaw : input.getThreepids()) {
                        ThreePidMapping mapping = new ThreePidMapping()
                        mapping.setMedium(mappingRaw.get(0))
                        mapping.setValue(mappingRaw.get(1))
                        mapping.setMxid(mappingRaw.get(2))
                        mappingsFound.add(mapping)
                    }
                } else {
                    log.info("HTTP response from {} was empty", remote)
                }

                return mappingsFound
            } finally {
                response.close()
            }
        } finally {
            client.close()
        }
    }

}
