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
import io.kamax.mxisd.config.ldap.LdapAttributeConfig;
import io.kamax.mxisd.config.ldap.LdapConfig;
import io.kamax.mxisd.controller.directory.v1.io.UserDirectorySearchResult;
import io.kamax.mxisd.directory.IDirectoryProvider;
import io.kamax.mxisd.exception.InternalServerError;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.CursorLdapReferralException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
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
import java.util.Collections;
import java.util.List;

@Component
public class LdapDirectoryProvider extends LdapGenericBackend implements IDirectoryProvider {

    private Logger log = LoggerFactory.getLogger(LdapDirectoryProvider.class);

    @Autowired
    public LdapDirectoryProvider(LdapConfig cfg, MatrixConfig mxCfg) {
        super(cfg, mxCfg);
    }

    @Override
    public boolean isEnabled() {
        return getCfg().isEnabled();
    }

    protected UserDirectorySearchResult search(String query, List<String> attributes) {
        UserDirectorySearchResult result = new UserDirectorySearchResult();
        result.setLimited(false);

        try (LdapConnection conn = getConn()) {
            bind(conn);

            LdapAttributeConfig atCfg = getCfg().getAttribute();

            attributes = new ArrayList<>(attributes);
            attributes.add(getUidAtt());
            String[] attArray = new String[attributes.size()];
            attributes.toArray(attArray);

            String searchQuery = buildOrQueryWithFilter(getCfg().getDirectory().getFilter(), "*" + query + "*", attArray);
            try (EntryCursor cursor = conn.search(getBaseDn(), searchQuery, SearchScope.SUBTREE, attArray)) {
                while (cursor.next()) {
                    Entry entry = cursor.get();
                    log.info("Found possible match, DN: {}", entry.getDn().getName());
                    getAttribute(entry, getUidAtt()).ifPresent(uid -> {
                        log.info("DN {} is a valid match", entry.getDn().getName());
                        try {
                            UserDirectorySearchResult.Result entryResult = new UserDirectorySearchResult.Result();
                            entryResult.setUserId(buildMatrixIdFromUid(uid));
                            getAttribute(entry, atCfg.getName()).ifPresent(entryResult::setDisplayName);
                            result.addResult(entryResult);
                        } catch (IllegalArgumentException e) {
                            log.warn("Bind was found but type {} is not supported", atCfg.getUid().getType());
                        }
                    });
                }
            }
        } catch (CursorLdapReferralException e) {
            log.warn("An entry is only available via referral, skipping");
        } catch (IOException | LdapException | CursorException e) {
            throw new InternalServerError(e);
        }

        return result;
    }

    @Override
    public UserDirectorySearchResult searchByDisplayName(String query) {
        log.info("Performing LDAP directory search on display name using '{}'", query);
        return search(query, Collections.singletonList(getCfg().getAttribute().getName()));
    }

    @Override
    public UserDirectorySearchResult searchBy3pid(String query) {
        log.info("Performing LDAP directory search on 3PIDs using '{}'", query);
        List<String> attributes = new ArrayList<>();
        getCfg().getAttribute().getThreepid().forEach((k, v) -> attributes.addAll(v));
        return search(query, attributes);
    }

}
