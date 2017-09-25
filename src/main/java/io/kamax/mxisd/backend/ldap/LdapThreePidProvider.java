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

package io.kamax.mxisd.backend.ldap;

import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.provider.IThreePidProvider;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class LdapThreePidProvider extends LdapGenericBackend implements IThreePidProvider {

    public static final String UID = "uid";
    public static final String MATRIX_ID = "mxid";

    private Logger log = LoggerFactory.getLogger(LdapThreePidProvider.class);

    @Autowired
    private MatrixConfig mxCfg;

    @Override
    public boolean isEnabled() {
        return getCfg().isEnabled();
    }

    private String getUidAttribute() {
        return getCfg().getAttribute().getUid().getValue();
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public int getPriority() {
        return 20;
    }

    private Optional<String> lookup(LdapConnection conn, String medium, String value) {
        String uidAttribute = getUidAttribute();

        Optional<String> queryOpt = getCfg().getIdentity().getQuery(medium);
        if (!queryOpt.isPresent()) {
            log.warn("{} is not a configured 3PID type for LDAP lookup", medium);
            return Optional.empty();
        }

        String searchQuery = queryOpt.get().replaceAll("%3pid", value);
        try (EntryCursor cursor = conn.search(getCfg().getConn().getBaseDn(), searchQuery, SearchScope.SUBTREE, uidAttribute)) {
            while (cursor.next()) {
                Entry entry = cursor.get();
                log.info("Found possible match, DN: {}", entry.getDn().getName());

                Attribute attribute = entry.get(uidAttribute);
                if (attribute == null) {
                    log.info("DN {}: no attribute {}, skpping", entry.getDn(), getCfg().getAttribute());
                    continue;
                }

                String data = attribute.get().toString();
                if (data.length() < 1) {
                    log.info("DN {}: empty attribute {}, skipping", getCfg().getAttribute());
                    continue;
                }

                StringBuilder matrixId = new StringBuilder();
                // TODO Should we turn this block into a map of functions?
                String uidType = getCfg().getAttribute().getUid().getType();
                if (StringUtils.equals(UID, uidType)) {
                    matrixId.append("@").append(data).append(":").append(mxCfg.getDomain());
                } else if (StringUtils.equals(MATRIX_ID, uidType)) {
                    matrixId.append(data);
                } else {
                    log.warn("Bind was found but type {} is not supported", uidType);
                    continue;
                }

                log.info("DN {} is a valid match", entry.getDn().getName());
                return Optional.of(matrixId.toString());
            }
        } catch (CursorLdapReferralException e) {
            log.warn("3PID {} is only available via referral, skipping", value);
        } catch (IOException | LdapException | CursorException e) {
            throw new InternalServerError(e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        log.info("Performing LDAP lookup ${request.getThreePid()} of type ${request.getType()}");

        try (LdapConnection conn = getConn()) {
            bind(conn);

            Optional<String> mxid = lookup(conn, request.getType(), request.getThreePid());
            if (mxid.isPresent()) {
                return Optional.of(new SingleLookupReply(request, mxid.get()));
            }
        } catch (LdapException | IOException e) {
            throw new InternalServerError(e);
        }

        log.info("No match found");
        return Optional.empty();
    }

    @Override
    public List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        log.info("Looking up {} mappings", mappings.size());
        List<ThreePidMapping> mappingsFound = new ArrayList<>();

        try (LdapConnection conn = getConn()) {
            bind(conn);

            for (ThreePidMapping mapping : mappings) {
                try {
                    Optional<String> mxid = lookup(conn, mapping.getMedium(), mapping.getValue());
                    if (mxid.isPresent()) {
                        mapping.setMxid(mxid.get());
                        mappingsFound.add(mapping);
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("{} is not a supported 3PID type for LDAP lookup", mapping.getMedium());
                }
            }
        } catch (LdapException | IOException e) {
            throw new InternalServerError(e);
        }

        return mappingsFound;
    }

}
