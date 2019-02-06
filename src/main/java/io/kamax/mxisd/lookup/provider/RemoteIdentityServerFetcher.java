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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.exception.InvalidResponseJsonException;
import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.io.identity.ClientBulkLookupRequest;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.fetcher.IRemoteIdentityServerFetcher;
import io.kamax.mxisd.matrix.IdentityServerUtils;
import io.kamax.mxisd.util.GsonParser;
import io.kamax.mxisd.util.RestClientUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RemoteIdentityServerFetcher implements IRemoteIdentityServerFetcher {

    private transient final Logger log = LoggerFactory.getLogger(RemoteIdentityServerFetcher.class);

    // FIXME remove
    private Gson gson = new Gson();
    private GsonParser parser = new GsonParser(gson);

    private CloseableHttpClient client;

    public RemoteIdentityServerFetcher(CloseableHttpClient client) {
        this.client = client;
    }

    @Override
    public boolean isUsable(String remote) {
        return IdentityServerUtils.isUsable(remote);
    }

    @Override
    public Optional<SingleLookupReply> find(String remote, SingleLookupRequest request) {
        log.info("Looking up {} 3PID {} using {}", request.getType(), request.getThreePid(), remote);

        try {
            URIBuilder b = new URIBuilder(remote);
            b.setPath(IsAPIv1.Base + "/lookup");
            b.addParameter("medium", request.getType());
            b.addParameter("address", request.getThreePid());
            HttpGet req = new HttpGet(b.build());

            try (CloseableHttpResponse res = client.execute(req)) {
                int statusCode = res.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(res.getEntity());

                if (statusCode != 200) {
                    log.warn("Remote returned status code {}", statusCode);
                    log.warn("Body: {}", body);
                    return Optional.empty();
                }

                JsonObject obj = GsonUtil.parseObj(body);
                if (obj.has("address")) {
                    log.debug("Found 3PID mapping: {}", gson.toJson(obj));
                    return Optional.of(SingleLookupReply.fromRecursive(request, gson.toJson(obj)));
                }

                log.info("Empty 3PID mapping from {}", remote);
                return Optional.empty();
            }
        } catch (IOException e) {
            log.warn("Error looking up 3PID mapping {}: {}", request.getThreePid(), e.getMessage());
            return Optional.empty();
        } catch (JsonParseException e) {
            log.warn("Invalid JSON answer from {}", remote);
            return Optional.empty();
        } catch (URISyntaxException e) {
            log.warn("Invalid remote address: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public List<ThreePidMapping> find(String remote, List<ThreePidMapping> mappings) {
        List<ThreePidMapping> mappingsFound = new ArrayList<>();

        ClientBulkLookupRequest mappingRequest = new ClientBulkLookupRequest();
        mappingRequest.setMappings(mappings);

        String url = remote + IsAPIv1.Base + "/bulk_lookup";
        try {
            HttpPost request = RestClientUtils.post(url, mappingRequest);
            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity());

                if (statusCode != 200) {
                    log.warn("Could not perform lookup at {} due to HTTP return code: {}", url, statusCode);
                    log.warn("Body: {}", body);
                    return mappingsFound;
                }

                ClientBulkLookupRequest input = parser.parse(response, ClientBulkLookupRequest.class);
                for (List<String> mappingRaw : input.getThreepids()) {
                    ThreePidMapping mapping = new ThreePidMapping();
                    mapping.setMedium(mappingRaw.get(0));
                    mapping.setValue(mappingRaw.get(1));
                    mapping.setMxid(mappingRaw.get(2));
                    mappingsFound.add(mapping);
                }
            }
        } catch (IOException e) {
            log.warn("Unable to fetch remote lookup data: {}", e.getMessage());
        } catch (InvalidResponseJsonException e) {
            log.info("HTTP response from {} was empty/invalid", remote);
        }

        return mappingsFound;
    }

}
