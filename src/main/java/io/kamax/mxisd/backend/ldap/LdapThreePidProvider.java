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

import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.ldap.LdapConfig;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.provider.IThreePidProvider;
import io.kamax.mxisd.util.GsonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.CursorLdapReferralException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LdapThreePidProvider extends LdapBackend implements IThreePidProvider {

    private transient final Logger log = LoggerFactory.getLogger(LdapThreePidProvider.class);

    public LdapThreePidProvider(LdapConfig cfg, MatrixConfig mxCfg) {
        super(cfg, mxCfg);
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
        Optional<String> tPidQueryOpt = getCfg().getIdentity().getQuery(medium);
        if (!tPidQueryOpt.isPresent()) {
            log.warn("{} is not a configured 3PID type for LDAP lookup", medium);
            return Optional.empty();
        }

        // we merge 3PID specific query with global/specific filter, if one exists.
        String tPidQuery = tPidQueryOpt.get().replaceAll(getCfg().getIdentity().getToken(), value);
        String searchQuery = buildWithFilter(tPidQuery, getCfg().getIdentity().getFilter());
        log.debug("Query: {}", searchQuery);
        log.debug("Attributes: {}", GsonUtil.build().toJson(getUidAtt()));

        for (String baseDN : getBaseDNs()) {
            log.debug("Base DN: {}", baseDN);

            try (EntryCursor cursor = conn.search(baseDN, searchQuery, SearchScope.SUBTREE, getUidAtt())) {
                while (cursor.next()) {
                    Entry entry = cursor.get();
                    log.info("Found possible match, DN: {}", entry.getDn().getName());

                    Optional<String> data = getAttribute(entry, getUidAtt());
                    if (!data.isPresent()) {
                        continue;
                    }

                    log.info("DN {} is a valid match", entry.getDn().getName());
                    return Optional.of(buildMatrixIdFromUid(data.get()));
                }
            } catch (CursorLdapReferralException e) {
                log.info("Got referral info: {}", e.getReferralInfo());

                return followReferral(medium, value, e.getReferralInfo());
                //log.warn("3PID {} is only available via referral, skipping", value);
            } catch (IOException | LdapException | CursorException e) {
                throw new InternalServerError(e);
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        log.info("Performing LDAP lookup {} of type {}", request.getThreePid(), request.getType());

        List<String> hosts = new ArrayList<>();

        String domain = getCfg().getConnection().getDomain();
        if (StringUtils.isNotBlank(domain)) {
            try {
                Record[] records = new Lookup("_ldap._tcp.DomainDnsZones." + domain, Type.SRV).run();
                if (records == null || records.length == 0) {
                    log.warn("No LDAP server found for domain {}", domain);
                    return Optional.empty();
                }

                for (Record record : records) {
                    if (record instanceof SRVRecord) {
                        SRVRecord srvRec = (SRVRecord) record;
                        hosts.add(srvRec.getTarget().toString(true));
                    }
                }

                if (hosts.isEmpty()) {
                    return Optional.empty();
                }
            } catch (TextParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            hosts.add(getCfg().getConnection().getHost());
        }

        for (String host : hosts) {
            log.info("Trying host {}", host);
            try (LdapConnection conn = getConn(host)) {
                bind(conn);
                Optional<SingleLookupReply> reply = lookup(conn, request.getType(), request.getThreePid()).map(id -> new SingleLookupReply(request, id));
                if (reply.isPresent()) return reply;
            } catch (LdapException | IOException e) {
                if (hosts.size() == 1) {
                    throw new InternalServerError(e);
                } else {
                    log.warn("Unable to query {}: {}", host, e.getMessage());
                }
            }
        }

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
                    lookup(conn, mapping.getMedium(), mapping.getValue()).ifPresent(id -> {
                        mapping.setMxid(id);
                        mappingsFound.add(mapping);
                    });
                } catch (IllegalArgumentException e) {
                    log.warn("{} is not a supported 3PID type for LDAP lookup", mapping.getMedium());
                }
            }
        } catch (LdapException | IOException e) {
            throw new InternalServerError(e);
        }

        return mappingsFound;
    }

    private Optional<String> followReferral(String medium, String value, String ref) {
        URI uri = URI.create(ref);

        Optional<String> tPidQueryOpt = getCfg().getIdentity().getQuery(medium);
        if (!tPidQueryOpt.isPresent()) {
            log.warn("{} is not a configured 3PID type for LDAP lookup", medium);
            return Optional.empty();
        }

        LdapConnection conn = getConn(uri.getHost());
        try {
            bind(conn);
        } catch (LdapException e) {
            throw new RuntimeException(e);
        }

        // we merge 3PID specific query with global/specific filter, if one exists.
        String tPidQuery = tPidQueryOpt.get().replaceAll(getCfg().getIdentity().getToken(), value);
        String searchQuery = buildWithFilter(tPidQuery, getCfg().getIdentity().getFilter());
        log.debug("Query: {}", searchQuery);
        log.debug("Attributes: {}", GsonUtil.build().toJson(getUidAtt()));

        try (EntryCursor cursor = conn.search(uri.getPath().substring(1), searchQuery, SearchScope.SUBTREE, getUidAtt())) {
            while (cursor.next()) {
                Entry entry = cursor.get();
                log.info("Found possible match, DN: {}", entry.getDn().getName());

                Optional<String> data = getAttribute(entry, getUidAtt());
                if (!data.isPresent()) {
                    continue;
                }

                log.info("DN {} is a valid match", entry.getDn().getName());
                return Optional.of(buildMatrixIdFromUid(data.get()));
            }

            return Optional.empty();
        } catch (CursorLdapReferralException e) {
            log.info("Got referral info: {}", e.getReferralInfo());

            return followReferral(medium, value, e.getReferralInfo());
            //log.warn("3PID {} is only available via referral, skipping", value);
        } catch (IOException | LdapException | CursorException e) {
            throw new InternalServerError(e);
        }
    }

}
