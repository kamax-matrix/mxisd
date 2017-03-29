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

package io.kamax.mxisd.lookup.provider

import io.kamax.mxisd.config.LdapConfig
import io.kamax.mxisd.config.ServerConfig
import io.kamax.mxisd.lookup.LookupRequest
import org.apache.commons.lang.StringUtils
import org.apache.directory.api.ldap.model.cursor.EntryCursor
import org.apache.directory.api.ldap.model.entry.Attribute
import org.apache.directory.api.ldap.model.message.SearchScope
import org.apache.directory.ldap.client.api.LdapConnection
import org.apache.directory.ldap.client.api.LdapNetworkConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class LdapProvider implements ThreePidProvider, InitializingBean {

    public static final String UID = "uid"
    public static final String MATRIX_ID = "mxid"

    private Logger log = LoggerFactory.getLogger(LdapProvider.class)

    @Autowired
    private ServerConfig srvCfg

    @Autowired
    private LdapConfig ldapCfg

    @Override
    void afterPropertiesSet() throws Exception {
        if (!Arrays.asList(UID, MATRIX_ID).contains(ldapCfg.getType())) {
            throw new IllegalArgumentException(ldapCfg.getType() + " is not a valid LDAP lookup type")
        }
    }

    @Override
    boolean isLocal() {
        return true
    }

    @Override
    int getPriority() {
        return 20
    }

    @Override
    Optional<?> find(LookupRequest request) {
        log.info("Performing LDAP lookup ${request.getThreePid()} of type ${request.getType()}")

        LdapConnection conn = new LdapNetworkConnection(ldapCfg.getHost(), ldapCfg.getPort())
        try {
            conn.bind(ldapCfg.getBindDn(), ldapCfg.getBindPassword())

            String searchQuery = ldapCfg.getQuery().replaceAll("%3pid", request.getThreePid())
            EntryCursor cursor = conn.search(ldapCfg.getBaseDn(), searchQuery, SearchScope.SUBTREE, ldapCfg.getAttribute())
            try {
                if (cursor.next()) {
                    Attribute attribute = cursor.get().get(ldapCfg.getAttribute())
                    if (attribute != null) {
                        String data = attribute.get().toString()
                        if (data.length() < 1) {
                            log.warn("Bind was found but value is empty")
                            return Optional.empty()
                        }

                        StringBuilder matrixId = new StringBuilder()
                        // TODO Should we turn this block into a map of functions?
                        if (StringUtils.equals(UID, ldapCfg.getType())) {
                            matrixId.append("@").append(data).append(":").append(srvCfg.getName())
                        }
                        if (StringUtils.equals(MATRIX_ID, ldapCfg.getType())) {
                            matrixId.append(data)
                        }

                        if (matrixId.length() < 1) {
                            log.warn("Bind was found but type ${ldapCfg.getType()} is not supported")
                            return Optional.empty()
                        }

                        return Optional.of([
                                address   : request.getThreePid(),
                                medium    : request.getType(),
                                mxid      : matrixId.toString(),
                                not_before: 0,
                                not_after : 9223372036854775807,
                                ts        : 0
                        ])
                    }
                }
            } finally {
                cursor.close()
            }
        } finally {
            conn.close()
        }

        log.info("No match found")
        return Optional.empty()
    }


}
