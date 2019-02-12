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

import com.google.gson.JsonObject;
import io.kamax.matrix.event.EventKey;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.matrix.json.MatrixJson;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.io.identity.SingeLookupReplyJson;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
import io.kamax.mxisd.storage.crypto.SignatureManager;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class SingleLookupHandler extends LookupHandler {

    public static final String Path = IsAPIv1.Base + "/lookup";

    private transient final Logger log = LoggerFactory.getLogger(SingleLookupHandler.class);

    private ServerConfig cfg;
    private LookupStrategy strategy;
    private SignatureManager signMgr;

    public SingleLookupHandler(MxisdConfig cfg, LookupStrategy strategy, SignatureManager signMgr) {
        this.cfg = cfg.getServer();
        this.strategy = strategy;
        this.signMgr = signMgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        String medium = getQueryParameter(exchange, "medium");
        String address = getQueryParameter(exchange, "address");

        SingleLookupRequest lookupRequest = new SingleLookupRequest();
        setRequesterInfo(lookupRequest, exchange);
        lookupRequest.setType(medium);
        lookupRequest.setThreePid(address);

        log.info("Got single lookup request from {} with client {} - Is recursive? {}",
                lookupRequest.getRequester(), lookupRequest.getUserAgent(), lookupRequest.isRecursive());

        Optional<SingleLookupReply> lookupOpt = strategy.find(lookupRequest);
        if (!lookupOpt.isPresent()) {
            log.info("No mapping was found, return empty JSON object");
            respondJson(exchange, "{}");
        } else {
            SingleLookupReply lookup = lookupOpt.get();

            // FIXME signing should be done in the business model, not in the controller
            JsonObject obj = GsonUtil.makeObj(new SingeLookupReplyJson(lookup));
            obj.add(EventKey.Signatures.get(), signMgr.signMessageGson(cfg.getName(), MatrixJson.encodeCanonical(obj)));

            respondJson(exchange, obj);
        }
    }

}
