/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Kamax Sarl
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

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import io.kamax.matrix.ThreePid;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.auth.provider.BackendAuthResult;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.ldap.LdapConfig;
import io.kamax.mxisd.util.GsonUtil;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class LdapAuthProvider extends LdapBackend implements AuthenticatorProvider {

    private Logger log = LoggerFactory.getLogger(LdapAuthProvider.class);

    private PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    @Autowired
    public LdapAuthProvider(LdapConfig cfg, MatrixConfig mxCfg) {
        super(cfg, mxCfg);
    }

    @Override
    public boolean isEnabled() {
        return getCfg().isEnabled();
    }

    private Optional<String> getMsisdn(String phoneNumber) {
        try { // FIXME export into dedicated ThreePid class within SDK (copy from Firebase Auth)
            return Optional.of(phoneUtil.format(
                    phoneUtil.parse(
                            phoneNumber,
                            null // No default region
                    ),
                    PhoneNumberUtil.PhoneNumberFormat.E164
            ).substring(1)); // We want without the leading +
        } catch (NumberParseException e) {
            log.warn("Invalid phone number: {}", phoneNumber);
            return Optional.empty();
        }
    }

    @Override
    public BackendAuthResult authenticate(_MatrixID mxid, String password) {
        log.info("Performing auth for {}", mxid);


        try (LdapConnection conn = getConn()) {
            bind(conn);

            String uidType = getAt().getUid().getType();
            String userFilterValue = StringUtils.equals(LdapBackend.UID, uidType) ? mxid.getLocalPart() : mxid.getId();
            if (StringUtils.isBlank(userFilterValue)) {
                log.warn("Username is empty, failing auth");
                return BackendAuthResult.failure();
            }

            String userFilter = "(" + getUidAtt() + "=" + userFilterValue + ")";
            userFilter = buildWithFilter(userFilter, getCfg().getAuth().getFilter());

            Set<String> attributes = new HashSet<>();
            attributes.add(getUidAtt());
            attributes.add(getAt().getName());
            getAt().getThreepid().forEach((k, v) -> attributes.addAll(v));
            String[] attArray = new String[attributes.size()];
            attributes.toArray(attArray);

            log.debug("Base DN: {}", getBaseDn());
            log.debug("Query: {}", userFilter);
            log.debug("Attributes: {}", GsonUtil.build().toJson(attArray));

            try (EntryCursor cursor = conn.search(getBaseDn(), userFilter, SearchScope.SUBTREE, attArray)) {
                while (cursor.next()) {
                    Entry entry = cursor.get();
                    String dn = entry.getDn().getName();
                    log.info("Checking possible match, DN: {}", dn);

                    if (!getAttribute(entry, getUidAtt()).isPresent()) {
                        continue;
                    }

                    log.info("Attempting authentication on LDAP for {}", dn);
                    try {
                        conn.bind(entry.getDn(), password);
                    } catch (LdapException e) {
                        log.info("Unable to bind using {} because {}", entry.getDn().getName(), e.getMessage());
                        return BackendAuthResult.failure();
                    }

                    Attribute nameAttribute = entry.get(getAt().getName());
                    String name = nameAttribute != null ? nameAttribute.get().toString() : null;

                    log.info("Authentication successful for {}", entry.getDn().getName());
                    log.info("DN {} is a valid match", dn);

                    // TODO should we canonicalize the MXID?
                    BackendAuthResult result = BackendAuthResult.success(mxid.getId(), UserIdType.MatrixID, name);
                    log.info("Processing 3PIDs for profile");
                    getAt().getThreepid().forEach((k, v) -> {
                        log.info("Processing 3PID type {}", k);
                        v.forEach(attId -> {
                            List<String> values = getAttributes(entry, attId);
                            log.info("\tAttribute {} has {} value(s)", attId, values.size());
                            getAttributes(entry, attId).forEach(tpidValue -> {
                                if (ThreePidMedium.PhoneNumber.is(k)) {
                                    tpidValue = getMsisdn(tpidValue).orElse(tpidValue);
                                }
                                result.withThreePid(new ThreePid(k, tpidValue));
                            });
                        });
                    });

                    log.info("Found {} 3PIDs", result.getProfile().getThreePids().size());
                    return result;
                }
            } catch (CursorLdapReferralException e) {
                log.warn("Entity for {} is only available via referral, skipping", mxid);
            }

            log.info("No match were found for {}", mxid);
            return BackendAuthResult.failure();
        } catch (LdapException | IOException | CursorException e) {
            throw new RuntimeException(e);
        }
    }

}
