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

import io.kamax.mxisd.config.ServerConfig
import io.kamax.mxisd.config.ldap.LdapConfig
import io.kamax.mxisd.lookup.SingleLookupRequest
import io.kamax.mxisd.lookup.ThreePidMapping
import org.apache.commons.lang.StringUtils
import org.apache.directory.api.ldap.model.cursor.CursorLdapReferralException
import org.apache.directory.api.ldap.model.cursor.EntryCursor
import org.apache.directory.api.ldap.model.entry.Attribute
import org.apache.directory.api.ldap.model.entry.Entry
import org.apache.directory.api.ldap.model.message.SearchScope
import org.apache.directory.ldap.client.api.LdapConnection
import org.apache.directory.ldap.client.api.LdapNetworkConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class LdapProvider implements IThreePidProvider {

    public static final String UID = "uid"
    public static final String MATRIX_ID = "mxid"

    private Logger log = LoggerFactory.getLogger(LdapProvider.class)

    @Autowired
    private ServerConfig srvCfg

    @Autowired
    private LdapConfig ldapCfg

    @Override
    boolean isEnabled() {
        return ldapCfg.isEnabled()
    }

    private LdapConnection getConn() {
        return new LdapNetworkConnection(ldapCfg.getConn().getHost(), ldapCfg.getConn().getPort(), ldapCfg.getConn().isTls())
    }

    private void bind(LdapConnection conn) {
        conn.bind(ldapCfg.getConn().getBindDn(), ldapCfg.getConn().getBindPassword())
    }

    private String getUidAttribute() {
        return ldapCfg.getAttribute().getUid().getValue();
    }

    @Override
    boolean isLocal() {
        return true
    }

    @Override
    int getPriority() {
        return 20
    }

    Optional<String> lookup(LdapConnection conn, String medium, String value) {
        String uidAttribute = getUidAttribute()

        Optional<String> queryOpt = ldapCfg.getIdentity().getQuery(medium)
        if (!queryOpt.isPresent()) {
            log.warn("{} is not a configured 3PID type for LDAP lookup", medium)
            return Optional.empty()
        }

        String searchQuery = queryOpt.get().replaceAll("%3pid", value)
        EntryCursor cursor = conn.search(ldapCfg.getConn().getBaseDn(), searchQuery, SearchScope.SUBTREE, uidAttribute)
        try {
            while (cursor.next()) {
                Entry entry = cursor.get()
                log.info("Found possible match, DN: {}", entry.getDn().getName())

                Attribute attribute = entry.get(uidAttribute)
                if (attribute == null) {
                    log.info("DN {}: no attribute {}, skpping", entry.getDn(), ldapCfg.getAttribute())
                    continue
                }

                String data = attribute.get().toString()
                if (data.length() < 1) {
                    log.info("DN {}: empty attribute {}, skipping", ldapCfg.getAttribute())
                    continue
                }

                StringBuilder matrixId = new StringBuilder()
                // TODO Should we turn this block into a map of functions?
                String uidType = ldapCfg.getAttribute().getUid().getType()
                if (StringUtils.equals(UID, uidType)) {
                    matrixId.append("@").append(data).append(":").append(srvCfg.getName())
                } else if (StringUtils.equals(MATRIX_ID, uidType)) {
                    matrixId.append(data)
                } else {
                    log.warn("Bind was found but type {} is not supported", uidType)
                    continue
                }

                log.info("DN {} is a valid match", entry.getDn().getName())
                return Optional.of(matrixId.toString())
            }
        } catch (CursorLdapReferralException e) {
            log.warn("3PID {} is only available via referral, skipping", value)
        } finally {
            cursor.close()
        }

        return Optional.empty()
    }

    @Override
    Optional<?> find(SingleLookupRequest request) {
        log.info("Performing LDAP lookup ${request.getThreePid()} of type ${request.getType()}")

        LdapConnection conn = getConn()
        try {
            bind(conn)

            Optional<String> mxid = lookup(conn, request.getType(), request.getThreePid())
            if (mxid.isPresent()) {
                return Optional.of([
                        address   : request.getThreePid(),
                        medium    : request.getType(),
                        mxid      : mxid.get(),
                        not_before: 0,
                        not_after : 9223372036854775807,
                        ts        : 0
                ])
            }
        } finally {
            conn.close()
        }

        log.info("No match found")
        return Optional.empty()
    }

    @Override
    List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        log.info("Looking up {} mappings", mappings.size())
        List<ThreePidMapping> mappingsFound = new ArrayList<>()

        LdapConnection conn = getConn()
        try {
            bind(conn)

            for (ThreePidMapping mapping : mappings) {
                try {
                    Optional<String> mxid = lookup(conn, mapping.getMedium(), mapping.getValue())
                    if (mxid.isPresent()) {
                        mapping.setMxid(mxid.get())
                        mappingsFound.add(mapping)
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("{} is not a supported 3PID type for LDAP lookup", mapping.getMedium())
                }
            }
        } finally {
            conn.close()
        }

        return mappingsFound
    }

}
