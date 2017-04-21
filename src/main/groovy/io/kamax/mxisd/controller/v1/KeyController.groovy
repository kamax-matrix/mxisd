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

import groovy.json.JsonOutput
import io.kamax.mxisd.exception.BadRequestException
import io.kamax.mxisd.exception.NotImplementedException
import io.kamax.mxisd.key.KeyManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletRequest

import static org.springframework.web.bind.annotation.RequestMethod.GET

@RestController
class KeyController {

    private Logger log = LoggerFactory.getLogger(KeyController.class)

    @Autowired
    private KeyManager keyMgr

    @RequestMapping(value = "/_matrix/identity/api/v1/pubkey/{keyType}:{keyId}", method = GET)
    String getKey(@PathVariable String keyType, @PathVariable int keyId) {
        if (!"ed25519".contentEquals(keyType)) {
            throw new BadRequestException("Invalid algorithm: " + keyType)
        }

        return JsonOutput.toJson([
                public_key: keyMgr.getPublicKeyBase64(keyId)
        ])
    }

    @RequestMapping(value = "/_matrix/identity/api/v1/pubkey/ephemeral/isvalid", method = GET)
    String checkEphemeralKeyValidity(HttpServletRequest request) {
        log.error("{} was requested but not implemented", request.getRequestURL())

        throw new NotImplementedException()
    }

    @RequestMapping(value = "/_matrix/identity/api/v1/pubkey/isvalid", method = GET)
    String checkKeyValidity(HttpServletRequest request) {
        log.error("{} was requested but not implemented", request.getRequestURL())

        throw new NotImplementedException()
    }

}
