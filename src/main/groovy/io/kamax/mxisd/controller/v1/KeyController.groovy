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

package io.kamax.mxisd.controller.v1

import com.google.gson.Gson
import groovy.json.JsonOutput
import io.kamax.mxisd.controller.v1.io.KeyValidityJson
import io.kamax.mxisd.exception.BadRequestException
import io.kamax.mxisd.key.KeyManager
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletRequest

import static org.springframework.web.bind.annotation.RequestMethod.GET

@RestController
@CrossOrigin
@RequestMapping(path = "/_matrix/identity/api/v1", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
class KeyController {

    private Logger log = LoggerFactory.getLogger(KeyController.class)

    @Autowired
    private KeyManager keyMgr

    private Gson gson = new Gson();
    private String validKey = gson.toJson(new KeyValidityJson(true));
    private String invalidKey = gson.toJson(new KeyValidityJson(false));

    @RequestMapping(value = "/pubkey/{keyType}:{keyId}", method = GET)
    String getKey(@PathVariable String keyType, @PathVariable int keyId) {
        if (!"ed25519".contentEquals(keyType)) {
            throw new BadRequestException("Invalid algorithm: " + keyType)
        }

        log.info("Key {}:{} was requested", keyType, keyId)
        return JsonOutput.toJson([
                public_key: keyMgr.getPublicKeyBase64(keyId)
        ])
    }

    @RequestMapping(value = "/pubkey/ephemeral/isvalid", method = GET)
    String checkEphemeralKeyValidity(HttpServletRequest request) {
        log.warn("Ephemeral key was request but no ephemeral key are generated, replying not valid")

        return invalidKey
    }

    @RequestMapping(value = "/pubkey/isvalid", method = GET)
    String checkKeyValidity(HttpServletRequest request, @RequestParam("public_key") String pubKey) {
        log.info("Validating public key {}", pubKey)

        // TODO do in manager
        boolean valid = StringUtils.equals(pubKey, keyMgr.getPublicKeyBase64(keyMgr.getCurrentIndex()))
        return valid ? validKey : invalidKey
    }

}
