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

package io.kamax.mxisd.crypto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kamax.matrix.event.EventKey;
import io.kamax.matrix.json.MatrixJson;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public interface SignatureManager {

    /**
     * Sign the message and add the signature to the <code>signatures</code> key.
     * <p>
     * If the key does not exist yet, it is created. If the key exist, the produced signature will be merged with any
     * existing ones.
     *
     * @param domain  The domain under which the signature should be added
     * @param message The message to sign and add the produced signature to
     * @return The provided message with the new signature
     * @throws IllegalArgumentException If the <code>signatures</code> value is not a JSON object
     */
    default JsonObject signMessageGson(String domain, JsonObject message) throws IllegalArgumentException {
        JsonElement signEl = message.remove(EventKey.Signatures.get());
        JsonObject oldSigns = new JsonObject();
        if (!Objects.isNull(signEl)) {
            if (!signEl.isJsonObject()) {
                throw new IllegalArgumentException("Message contains a signatures key that is not a JSON object value");
            }

            oldSigns = signEl.getAsJsonObject();
        }

        JsonObject newSigns = signMessageGson(domain, MatrixJson.encodeCanonical(message));
        oldSigns.entrySet().forEach(entry -> newSigns.add(entry.getKey(), entry.getValue()));
        message.add(EventKey.Signatures.get(), newSigns);

        return message;
    }

    /**
     * Sign the message and produce a <code>signatures</code> object that can directly be added to the object being signed.
     *
     * @param domain  The domain under which the signature should be added
     * @param message The message to sign
     * @return The <code>signatures</code> object
     */
    JsonObject signMessageGson(String domain, String message);

    /**
     * Sign the canonical form of a JSON object.
     *
     * @param obj The JSON object to canonicalize and sign
     * @return The signature
     */
    Signature sign(JsonObject obj);

    /**
     * Sign the message, using UTF-8 as decoding character set.
     *
     * @param message The UTF-8 encoded message
     * @return The signature
     */
    default Signature sign(String message) {
        return sign(message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sign the data.
     *
     * @param data The data to sign
     * @return The signature
     */
    Signature sign(byte[] data);

}
