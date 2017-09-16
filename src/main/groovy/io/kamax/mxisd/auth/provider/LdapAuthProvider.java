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

package io.kamax.mxisd.auth.provider;

import io.kamax.matrix.MatrixID;
import io.kamax.mxisd.auth.UserAuthResult;
import io.kamax.mxisd.backend.LdapBackend;
import io.kamax.mxisd.lookup.provider.LdapProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.CursorLdapReferralException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LdapAuthProvider extends LdapBackend implements AuthenticatorProvider {

    private Logger log = LoggerFactory.getLogger(LdapAuthProvider.class);

    private String getUidAttribute() {
        return getCfg().getAttribute().getUid().getValue();
    }

    @Override
    public boolean isEnabled() {
        return getCfg().isEnabled();
    }

    @Override
    public UserAuthResult authenticate(String id, String password) {
        log.info("Performing auth for {}", id);

        LdapConnection conn = getConn();
        try {
            bind(conn);

            String uidType = getCfg().getAttribute().getUid().getType();
            MatrixID mxIdExt = new MatrixID(id);
            String userFilterValue = StringUtils.equals(LdapProvider.UID, uidType) ? mxIdExt.getLocalPart() : mxIdExt.getId();
            String userFilter = "(" + getCfg().getAttribute().getUid().getValue() + "=" + userFilterValue + ")";
            EntryCursor cursor = conn.search(getCfg().getConn().getBaseDn(), userFilter, SearchScope.SUBTREE, getUidAttribute(), getCfg().getAttribute().getName());
            try {
                while (cursor.next()) {
                    Entry entry = cursor.get();
                    String dn = entry.getDn().getName();
                    log.info("Checking possible match, DN: {}", dn);

                    Attribute attribute = entry.get(getUidAttribute());
                    if (attribute == null) {
                        log.info("DN {}: no attribute {}, skpping", dn, getUidAttribute());
                        continue;
                    }

                    String data = attribute.get().toString();
                    if (data.length() < 1) {
                        log.info("DN {}: empty attribute {}, skipping", getUidAttribute());
                        continue;
                    }

                    log.info("Attempting authentication on LDAP for {}", dn);
                    try {
                        conn.bind(entry.getDn(), password);
                    } catch (LdapException e) {
                        log.info("Unable to bind using {} because {}", entry.getDn().getName(), e.getMessage());
                        return new UserAuthResult().failure();
                    }

                    Attribute nameAttribute = entry.get(getCfg().getAttribute().getName());
                    String name = nameAttribute != null ? nameAttribute.get().toString() : null;

                    log.info("Authentication successful for {}", entry.getDn().getName());
                    log.info("DN {} is a valid match", dn);

                    return new UserAuthResult().success(mxIdExt.getId(), name);
                }
            } catch (CursorLdapReferralException e) {
                log.warn("Entity for {} is only available via referral, skipping", mxIdExt);
            } finally {
                cursor.close();
            }

            log.info("No match were found for {}", id);
            return new UserAuthResult().failure();
        } catch (LdapException | IOException | CursorException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                conn.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
