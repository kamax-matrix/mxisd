/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://www.kamax.io/
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

package io.kamax.mxisd.backend.ldap;

import io.kamax.matrix.ThreePid;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix._ThreePid;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.ldap.LdapConfig;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.profile.ProfileProvider;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.CursorLdapReferralException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LdapProfileProvider extends LdapBackend implements ProfileProvider {

    private transient final Logger log = LoggerFactory.getLogger(LdapProfileProvider.class);

    public LdapProfileProvider(LdapConfig cfg, MatrixConfig mxCfg) {
        super(cfg, mxCfg);
    }

    @Override
    public Optional<String> getDisplayName(_MatrixID userId) {
        String uid = buildUidFromMatrixId(userId);
        log.info("Searching for display name of {}:", uid);

        try (LdapConnection conn = getConn()) {
            bind(conn);

            String searchQuery = buildOrQueryWithFilter(getCfg().getProfile().getFilter(), uid, getUidAtt());
            log.debug("Query: {}", searchQuery);

            for (String baseDN : getBaseDNs()) {
                log.debug("Base DN: {}", baseDN);
                try (EntryCursor cursor = conn.search(baseDN, searchQuery, SearchScope.SUBTREE, getAt().getName())) {
                    while (cursor.next()) {
                        Entry entry = cursor.get();
                        log.info("Found possible match, DN: {}", entry.getDn().getName());
                        Optional<String> v = getAttribute(entry, getAt().getName()).flatMap(id -> {
                            log.info("DN {} is a valid match", entry.getDn().getName());
                            try {
                                return getAttribute(entry, getAt().getName());
                            } catch (IllegalArgumentException e) {
                                log.warn("Bind was found but type {} is not supported", getAt().getUid().getType());
                                return Optional.empty();
                            }
                        });

                        if (v.isPresent()) {
                            log.info("DN {} is the final match", entry.getDn().getName());
                            return v;
                        }
                    }
                } catch (CursorLdapReferralException e) {
                    log.warn("An entry is only available via referral, skipping");
                }
            }
        } catch (IOException | LdapException | CursorException e) {
            throw new InternalServerError(e);
        }

        return Optional.empty();
    }

    @Override
    public List<_ThreePid> getThreepids(_MatrixID userId) {
        String uid = buildUidFromMatrixId(userId);
        log.info("Searching for 3PIDs of {}:", uid);

        List<_ThreePid> threePids = new ArrayList<>();
        try (LdapConnection conn = getConn()) {
            bind(conn);

            getCfg().getAttribute().getThreepid().forEach((medium, attributes) -> {
                String[] attArray = new String[attributes.size()];
                attributes.toArray(attArray);

                String searchQuery = buildOrQueryWithFilter(getCfg().getProfile().getFilter(), uid, getUidAtt());

                log.debug("Query for 3PID {}: {}", medium, searchQuery);

                for (String baseDN : getBaseDNs()) {
                    log.debug("Base DN: {}", baseDN);
                    try (EntryCursor cursor = conn.search(baseDN, searchQuery, SearchScope.SUBTREE, attArray)) {
                        while (cursor.next()) {
                            Entry entry = cursor.get();
                            log.info("Found possible match, DN: {}", entry.getDn().getName());
                            try {
                                attributes.stream()
                                        .flatMap(at -> getAttributes(entry, at).stream())
                                        .forEach(address -> {
                                            log.info("Found 3PID: {} - {}", medium, address);
                                            threePids.add(new ThreePid(medium, address));
                                        });
                            } catch (IllegalArgumentException e) {
                                log.warn("Bind was found but type {} is not supported", getAt().getUid().getType());
                            }
                        }
                    } catch (CursorLdapReferralException e) {
                        log.warn("An entry is only available via referral, skipping");
                    } catch (LdapException | IOException | CursorException e) {
                        throw new InternalServerError(e);
                    }
                }
            });
        } catch (IOException | LdapException e) {
            throw new InternalServerError(e);
        }

        return threePids;
    }

    @Override
    public List<String> getRoles(_MatrixID userId) {
        return Collections.emptyList();
    }

}
