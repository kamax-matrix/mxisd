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

package io.kamax.mxisd.controller

import groovy.json.JsonOutput
import io.kamax.mxisd.config.LdapConfig
import io.kamax.mxisd.exception.BadRequestException
import io.kamax.mxisd.signature.SignatureManager
import org.apache.directory.api.ldap.model.cursor.EntryCursor
import org.apache.directory.api.ldap.model.entry.Attribute
import org.apache.directory.api.ldap.model.message.SearchScope
import org.apache.directory.ldap.client.api.LdapConnection
import org.apache.directory.ldap.client.api.LdapNetworkConnection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import static org.springframework.web.bind.annotation.RequestMethod.GET

@RestController
class MappingController {

    @Autowired
    private LdapConfig ldapCfg

    @Autowired
    private SignatureManager signMgr

    @RequestMapping(value = "/_matrix/identity/api/v1/lookup", method = GET)
    String lookup(@RequestParam String medium, @RequestParam String address) {
        if (!"email".contentEquals(medium)) {
            throw new BadRequestException("Invalid medium type")
        }

        LdapConnection conn = new LdapNetworkConnection(ldapCfg.getHost(), ldapCfg.getPort())
        try {
            conn.bind(ldapCfg.getBindDn(), ldapCfg.getBindPassword())

            String searchQuery = ldapCfg.getQuery().replaceAll("%3pid", address)
            EntryCursor cursor = conn.search(ldapCfg.getBaseDn(), searchQuery, SearchScope.SUBTREE, ldapCfg.getAttribute())
            try {
                if (!cursor.next()) {
                    return JsonOutput.toJson([])
                }

                Attribute attribute = cursor.get().get(ldapCfg.getAttribute())
                if (attribute == null) {
                    return JsonOutput.toJson([])
                }

                def data = new LinkedHashMap([
                        address   : address,
                        medium    : medium,
                        mxid      : attribute.get().toString(),
                        not_before: 0,
                        not_after : 9223372036854775807,
                        ts        : 0
                ])

                data['signatures'] = signMgr.signMessage(JsonOutput.toJson(data))
                return JsonOutput.toJson(data)
            } finally {
                cursor.close()
            }
        } finally {
            conn.close()
        }
    }

}
