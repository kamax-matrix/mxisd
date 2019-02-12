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
import io.kamax.matrix.codec.MxBase64;
import io.kamax.matrix.json.MatrixJson;
import net.i2p.crypto.eddsa.EdDSAEngine;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

public class Ed25519SignatureManager implements SignatureManager {

    private final Ed25519KeyManager keyMgr;

    public Ed25519SignatureManager(Ed25519KeyManager keyMgr) {
        this.keyMgr = keyMgr;
    }

    @Override
    public JsonObject signMessageGson(String domain, String message) {
        Signature sign = sign(message);

        JsonObject keySignature = new JsonObject();
        // FIXME should create a signing key object what would give this ed and index values
        keySignature.addProperty(sign.getKey().getAlgorithm() + ":" + sign.getKey().getSerial(), sign.getSignature());
        JsonObject signature = new JsonObject();
        signature.add(domain, keySignature);

        return signature;
    }

    @Override
    public Signature sign(JsonObject obj) {

        return sign(MatrixJson.encodeCanonical(obj));
    }

    @Override
    public Signature sign(byte[] data) {
        try {
            KeyIdentifier signingKeyId = keyMgr.getServerSigningKey().getId();
            EdDSAEngine signEngine = new EdDSAEngine(MessageDigest.getInstance(keyMgr.getKeySpecs().getHashAlgorithm()));
            signEngine.initSign(keyMgr.getPrivateKey(signingKeyId));
            byte[] signRaw = signEngine.signOneShot(data);
            String sign = MxBase64.encode(signRaw);

            return new Signature() {
                @Override
                public KeyIdentifier getKey() {
                    return signingKeyId;
                }

                @Override
                public String getSignature() {
                    return sign;
                }
            };
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

}
