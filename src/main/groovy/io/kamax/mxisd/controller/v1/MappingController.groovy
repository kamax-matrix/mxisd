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
import io.kamax.mxisd.api.ThreePidType
import io.kamax.mxisd.lookup.LookupStrategy
import io.kamax.mxisd.signature.SignatureManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import static org.springframework.web.bind.annotation.RequestMethod.GET

@RestController
class MappingController {

    @Autowired
    private LookupStrategy strategy

    @Autowired
    private SignatureManager signMgr

    @RequestMapping(value = "/_matrix/identity/api/v1/lookup", method = GET)
    String lookup(@RequestParam String medium, @RequestParam String address) {
        ThreePidType type = ThreePidType.valueOf(medium)
        Optional<?> lookupOpt = strategy.find(type, address)
        if (!lookupOpt.isPresent()) {
            return JsonOutput.toJson([])
        }

        def lookup = lookupOpt.get()
        if (lookup['signatures'] == null) {
            lookup['signatures'] = signMgr.signMessage(JsonOutput.toJson(lookup))
        }

        return JsonOutput.toJson(lookup)
    }

}
