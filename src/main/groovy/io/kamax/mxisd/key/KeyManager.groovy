/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
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

package io.kamax.mxisd.key

import io.kamax.mxisd.config.KeyConfig
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.KeyPairGenerator
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyPair
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey

@Component
class KeyManager implements InitializingBean {

    @Autowired
    private KeyConfig keyCfg

    private EdDSAParameterSpec keySpecs
    private EdDSAEngine signEngine
    private List<KeyPair> keys

    @Override
    void afterPropertiesSet() throws Exception {
        keySpecs = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.CURVE_ED25519_SHA512)
        signEngine = new EdDSAEngine(MessageDigest.getInstance(keySpecs.getHashAlgorithm()))
        keys = new ArrayList<>()

        Path privKey = Paths.get(keyCfg.getPath())

        if (!Files.exists(privKey)) {
            KeyPair pair = (new KeyPairGenerator()).generateKeyPair()
            String keyEncoded = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded())
            FileUtils.writeStringToFile(privKey.toFile(), keyEncoded, StandardCharsets.ISO_8859_1)
            keys.add(pair)
        } else {
            if (Files.isDirectory(privKey)) {
                throw new RuntimeException("Invalid path for private key: ${privKey.toString()}")
            }

            if (Files.isReadable(privKey)) {
                byte[] seed = Base64.getDecoder().decode(FileUtils.readFileToString(privKey.toFile(), StandardCharsets.ISO_8859_1))
                EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(seed, keySpecs)
                EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(privKeySpec.getA(), keySpecs)
                keys.add(new KeyPair(new EdDSAPublicKey(pubKeySpec), new EdDSAPrivateKey(privKeySpec)))
            }
        }
    }

    int getCurrentIndex() {
        return 0
    }

    KeyPair getKeys(int index) {
        return keys.get(index)
    }

    PrivateKey getPrivateKey(int index) {
        return getKeys(index).getPrivate()
    }

    PublicKey getPublicKey(int index) {
        return getKeys(index).getPublic()
    }

    EdDSAParameterSpec getSpecs() {
        return keySpecs
    }

    String getPublicKeyBase64(int index) {
        return Base64.getEncoder().encodeToString(getPublicKey(index).getEncoded())
    }

}
