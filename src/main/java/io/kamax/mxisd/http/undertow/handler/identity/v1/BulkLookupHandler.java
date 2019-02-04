/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
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

package io.kamax.mxisd.http.undertow.handler.identity.v1;

import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.io.identity.ClientBulkLookupAnswer;
import io.kamax.mxisd.http.io.identity.ClientBulkLookupRequest;
import io.kamax.mxisd.lookup.BulkLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BulkLookupHandler extends LookupHandler {

    public static final String Path = IsAPIv1.Base + "/bulk_lookup";

    private transient final Logger log = LoggerFactory.getLogger(SingleLookupHandler.class);

    private LookupStrategy strategy;

    public BulkLookupHandler(LookupStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        ClientBulkLookupRequest input = parseJsonTo(exchange, ClientBulkLookupRequest.class);
        BulkLookupRequest lookupRequest = new BulkLookupRequest();
        setRequesterInfo(lookupRequest, exchange);
        log.info("Got bulk lookup request from {} with client {} - Is recursive? {}",
                lookupRequest.getRequester(), lookupRequest.getUserAgent(), lookupRequest.isRecursive());

        List<ThreePidMapping> mappings = new ArrayList<>();
        for (List<String> mappingRaw : input.getThreepids()) {
            ThreePidMapping mapping = new ThreePidMapping();
            mapping.setMedium(mappingRaw.get(0));
            mapping.setValue(mappingRaw.get(1));
            mappings.add(mapping);
        }
        lookupRequest.setMappings(mappings);

        ClientBulkLookupAnswer answer = new ClientBulkLookupAnswer();
        answer.addAll(strategy.find(lookupRequest).get());
        log.info("Finished bulk lookup request from {}", lookupRequest.getRequester());

        respondJson(exchange, answer);
    }

}
