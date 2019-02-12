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
import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.storage.crypto.GenericKeyIdentifier;
import io.kamax.mxisd.storage.crypto.KeyManager;
import io.kamax.mxisd.storage.crypto.KeyType;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyGetHandler extends BasicHttpHandler {

    public static final String Key = "key";
    public static final String Path = IsAPIv1.Base + "/pubkey/{key}";

    private transient final Logger log = LoggerFactory.getLogger(KeyGetHandler.class);

    private KeyManager mgr;

    public KeyGetHandler(KeyManager mgr) {
        this.mgr = mgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        String key = getQueryParameter(exchange, Key);
        String[] v = key.split(":", 2);
        String keyAlgo = v[0];
        String keyId = v[1];

        log.info("Key {}:{} was requested", keyAlgo, keyId);
        JsonObject obj = new JsonObject();
        obj.addProperty("public_key", mgr.getPublicKeyBase64(new GenericKeyIdentifier(KeyType.Regular, keyAlgo, keyId)));
        respond(exchange, obj);
    }

}
