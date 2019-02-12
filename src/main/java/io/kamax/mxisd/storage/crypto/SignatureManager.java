/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2019 Kamax Sàrl
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

package io.kamax.mxisd.storage.crypto;

import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;

public interface SignatureManager {

    JsonObject signMessageGson(String domain, String message);

    /**
     * Sign the canonical form of a JSON object
     *
     * @param obj The JSON object to canonicalize and sign
     * @return The signature
     */
    Signature sign(JsonObject obj);

    /**
     * Sign the message, using UTF-8 as decoding character set
     *
     * @param message The UTF-8 encoded message
     * @return
     */
    default Signature sign(String message) {
        return sign(message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sign the data
     *
     * @param data The data to sign
     * @return The signature
     */
    Signature sign(byte[] data);

}
