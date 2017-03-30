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

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonBuilder
import io.kamax.mxisd.api.ThreePidType
import io.kamax.mxisd.lookup.LookupRequest
import io.kamax.mxisd.lookup.strategy.LookupStrategy
import io.kamax.mxisd.signature.SignatureManager
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController



import javax.servlet.http.HttpServletRequest

import static org.springframework.web.bind.annotation.RequestMethod.GET
import static org.springframework.web.bind.annotation.RequestMethod.POST

@RestController
class MappingController {

    private Logger log = LoggerFactory.getLogger(MappingController.class)

    @Autowired
    private LookupStrategy strategy

    @Autowired
    private SignatureManager signMgr

    @RequestMapping(value = "/_matrix/identity/api/v1/lookup", method = GET)
    String lookup(HttpServletRequest request, @RequestParam String medium, @RequestParam String address) {
        String remote = StringUtils.defaultIfBlank(request.getHeader("X-FORWARDED-FOR"), request.getRemoteAddr())
        log.info("Got request from {}", remote)

        ThreePidType type = ThreePidType.valueOf(medium)

        LookupRequest lookupRequest = new LookupRequest()
        lookupRequest.setRequester(remote)
        lookupRequest.setType(type)
        lookupRequest.setThreePid(address)

        Optional<?> lookupOpt = strategy.find(lookupRequest)
        if (!lookupOpt.isPresent()) {
            log.info("No mapping was found, return empty JSON object")
            return JsonOutput.toJson([])
        }

        def lookup = lookupOpt.get()
        if (lookup['signatures'] == null) {
            log.info("lookup is not signed yet, we sign it")
            lookup['signatures'] = signMgr.signMessage(JsonOutput.toJson(lookup))
        }

        return JsonOutput.toJson(lookup)
    }

    @RequestMapping(value = "/_matrix/identity/api/v1/bulk_lookup", method = POST)
    String bulk_lookup(HttpServletRequest request, @RequestBody String post) {
        def slurper = new JsonSlurper()
        def jsn = slurper.parseText(post)

        String remote = StringUtils.defaultIfBlank(request.getHeader("X-FORWARDED-FOR"), request.getRemoteAddr())
        log.info("Got bulk_lookup request from {}", remote)

        def tpids = []

        for (List<String> threepid : jsn.threepids) {
            log.info("Request type: {} value: {}", threepid.get(0), threepid.get(1))
            ThreePidType type = ThreePidType.valueOf(threepid.get(0))
            LookupRequest lookupRequest = new LookupRequest()
            lookupRequest.setRequester(remote)
            lookupRequest.setType(type)
            lookupRequest.setThreePid(threepid.get(1))

            Optional<?> lookupOpt = strategy.find(lookupRequest)
            if (!lookupOpt.isPresent()) {
                log.info("No mapping was found")
            }
            else
            {
                def lookup = lookupOpt.get()
                log.info("Mapping found {}", lookup)
                tpids.add([lookup['medium'],lookup['address'],lookup['mxid']])
            }

        }

        def sign = signMgr.signMessage(JsonOutput.toJson(tpids))

        def build = new JsonBuilder()
        build{
            threepids(tpids)
        }

        return JsonOutput.toJson(build.content)
    }

}
