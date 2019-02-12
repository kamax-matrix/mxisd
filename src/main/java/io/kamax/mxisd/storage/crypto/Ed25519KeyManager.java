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

import io.kamax.matrix.codec.MxBase64;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.KeyPairGenerator;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.time.Instant;
import java.util.List;

public class Ed25519KeyManager implements KeyManager {

    private static final Logger log = LoggerFactory.getLogger(Ed25519KeyManager.class);

    private final EdDSAParameterSpec keySpecs;
    private final KeyStore store;

    public Ed25519KeyManager(KeyStore store) {
        this.keySpecs = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
        this.store = store;

        if (!store.getCurrentKey().isPresent()) {
            List<KeyIdentifier> keys = store.list(KeyType.Regular);
            if (keys.isEmpty()) {
                keys.add(generateKey(KeyType.Regular));
            }

            store.setCurrentKey(keys.get(0));
        }
    }

    protected String generateId() {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(Instant.now().toEpochMilli() - 1546297200000L); // TS since 2019-01-01T00:00:00Z to keep IDs short
        return Base64.encodeBase64URLSafeString(buffer.array()) + RandomStringUtils.randomAlphanumeric(1);
    }

    protected String getPrivateKeyBase64(EdDSAPrivateKey key) {
        return MxBase64.encode(key.getSeed());
    }

    public EdDSAParameterSpec getKeySpecs() {
        return keySpecs;
    }

    @Override
    public KeyIdentifier generateKey(KeyType type) {
        KeyIdentifier id;
        do {
            id = new GenericKeyIdentifier(type, KeyAlgorithm.Ed25519, generateId());
        } while (store.has(id));

        KeyPair pair = (new KeyPairGenerator()).generateKeyPair();
        String keyEncoded = getPrivateKeyBase64((EdDSAPrivateKey) pair.getPrivate());

        Key key = new GenericKey(id, true, keyEncoded);
        store.add(key);

        return id;
    }

    @Override
    public List<KeyIdentifier> getKeys(KeyType type) {
        return store.list(type);
    }

    @Override
    public Key getServerSigningKey() {
        return store.get(store.getCurrentKey().orElseThrow(IllegalStateException::new));
    }

    @Override
    public Key getKey(KeyIdentifier id) {
        return store.get(id);
    }

    public EdDSAPrivateKeySpec getPrivateKeySpecs(KeyIdentifier id) {
        return new EdDSAPrivateKeySpec(java.util.Base64.getDecoder().decode(getKey(id).getPrivateKeyBase64()), keySpecs);
    }

    public EdDSAPrivateKey getPrivateKey(KeyIdentifier id) {
        return new EdDSAPrivateKey(getPrivateKeySpecs(id));
    }

    public EdDSAPublicKey getPublicKey(KeyIdentifier id) {
        EdDSAPrivateKeySpec privKeySpec = getPrivateKeySpecs(id);
        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(privKeySpec.getA(), keySpecs);
        return new EdDSAPublicKey(pubKeySpec);
    }

    @Override
    public void disableKey(KeyIdentifier id) {
        Key key = store.get(id);
        key = new GenericKey(id, false, key.getPrivateKeyBase64());
        store.update(key);
    }

    @Override
    public String getPublicKeyBase64(KeyIdentifier id) {
        return MxBase64.encode(getPublicKey(id).getAbyte());
    }

    @Override
    public boolean isValid(KeyType type, String publicKeyBase64) {
        // TODO caching?
        return getKeys(type).stream().anyMatch(id -> StringUtils.equals(getPublicKeyBase64(id), publicKeyBase64));
    }

}
