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
import io.kamax.mxisd.storage.crypto.KeyManager;
import io.kamax.mxisd.storage.crypto.KeyType;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EphemeralKeyIsValidHandler extends KeyIsValidHandler {

    public static final String Path = IsAPIv1.Base + "/pubkey/ephemeral/isvalid";

    private transient final Logger log = LoggerFactory.getLogger(EphemeralKeyIsValidHandler.class);

    private KeyManager mgr;

    public EphemeralKeyIsValidHandler(KeyManager mgr) {
        this.mgr = mgr;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        // FIXME process + correctly in query parameter handling
        String pubKey = getQueryParameter(exchange, "public_key").replace(" ", "+");
        log.info("Validating ephemeral public key {}", pubKey);

        respondJson(exchange, mgr.isValid(KeyType.Ephemeral, pubKey) ? validKey : invalidKey);
    }

}
