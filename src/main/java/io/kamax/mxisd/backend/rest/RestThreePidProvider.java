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

package io.kamax.mxisd.backend.rest;

import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.UserID;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.rest.RestBackendConfig;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.provider.IThreePidProvider;
import io.kamax.mxisd.util.RestClientUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RestThreePidProvider extends RestProvider implements IThreePidProvider {

    private transient final Logger log = LoggerFactory.getLogger(RestThreePidProvider.class);

    private MatrixConfig mxCfg; // FIXME should be done in the lookup manager

    public RestThreePidProvider(RestBackendConfig cfg, MatrixConfig mxCfg) {
        super(cfg);
        this.mxCfg = mxCfg;
    }

    // TODO refactor in lookup manager with above FIXME
    private _MatrixID getMxId(UserID id) {
        if (UserIdType.Localpart.is(id.getType())) {
            return MatrixID.asAcceptable(id.getValue(), mxCfg.getDomain());
        } else {
            return MatrixID.asAcceptable(id.getValue());
        }
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public int getPriority() {
        return 20;
    }

    // TODO refactor common code
    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        String endpoint = cfg.getEndpoints().getIdentity().getSingle();
        HttpUriRequest req = RestClientUtils.post(endpoint, gson, "lookup",
                new LookupSingleRequestJson(request.getType(), request.getThreePid()));

        try (CloseableHttpResponse res = client.execute(req)) {
            int status = res.getStatusLine().getStatusCode();
            if (status < 200 || status >= 300) {
                log.warn("REST endpoint {} answered with status {}, no binding found", endpoint, status);
                return Optional.empty();
            }

            Optional<LookupSingleResponseJson> responseOpt = parser.parseOptional(res, "lookup", LookupSingleResponseJson.class);
            return responseOpt.map(lookupSingleResponseJson -> new SingleLookupReply(request, getMxId(lookupSingleResponseJson.getId())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO refactor common code
    @Override
    public List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        List<LookupSingleRequestJson> ioListRequest = mappings.stream()
                .map(mapping -> new LookupSingleRequestJson(mapping.getMedium(), mapping.getValue()))
                .collect(Collectors.toList());

        HttpUriRequest req = RestClientUtils.post(
                cfg.getEndpoints().getIdentity().getBulk(), gson, "lookup", ioListRequest);
        try (CloseableHttpResponse res = client.execute(req)) {
            mappings = new ArrayList<>();

            int status = res.getStatusLine().getStatusCode();
            if (status < 200 || status >= 300) {
                return mappings;
            }

            LookupBulkResponseJson listIo = parser.parse(res, LookupBulkResponseJson.class);
            return listIo.getLookup().stream()
                    .map(io -> new ThreePidMapping(io.getMedium(), io.getAddress(), getMxId(io.getId()).getId()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
