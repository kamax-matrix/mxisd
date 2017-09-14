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

package io.kamax.mxisd.signature

import com.google.gson.JsonObject
import io.kamax.mxisd.config.ServerConfig
import io.kamax.mxisd.key.KeyManager
import net.i2p.crypto.eddsa.EdDSAEngine
import org.json.JSONObject
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.security.MessageDigest

@Component
class SignatureManager implements InitializingBean {

    @Autowired
    private KeyManager keyMgr

    @Autowired
    private ServerConfig srvCfg

    private EdDSAEngine signEngine

    private String sign(String message) {
        byte[] signRaw = signEngine.signOneShot(message.getBytes())
        return Base64.getEncoder().encodeToString(signRaw)
    }

    JSONObject signMessageJson(String message) {
        String sign = sign(message)

        JSONObject keySignature = new JSONObject()
        keySignature.put("ed25519:${keyMgr.getCurrentIndex()}", sign)
        JSONObject signature = new JSONObject()
        signature.put("${srvCfg.getName()}", keySignature)

        return signature
    }

    JsonObject signMessageGson(String message) {
        String sign = sign(message)

        JsonObject keySignature = new JsonObject()
        keySignature.addProperty("ed25519:${keyMgr.getCurrentIndex()}", sign)
        JsonObject signature = new JsonObject()
        signature.add("${srvCfg.getName()}", keySignature);

        return signature
    }

    @Override
    void afterPropertiesSet() throws Exception {
        signEngine = new EdDSAEngine(MessageDigest.getInstance(keyMgr.getSpecs().getHashAlgorithm()))
        signEngine.initSign(keyMgr.getPrivateKey(keyMgr.getCurrentIndex()))
    }

}
