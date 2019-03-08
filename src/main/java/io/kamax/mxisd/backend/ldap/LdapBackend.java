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

import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.ldap.LdapConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.AttributeUtils;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public abstract class LdapBackend {

    public static final String UID = "uid";
    public static final String MATRIX_ID = "mxid";

    private transient final Logger log = LoggerFactory.getLogger(LdapBackend.class);

    private LdapConfig cfg;
    private MatrixConfig mxCfg;

    public LdapBackend(LdapConfig cfg, MatrixConfig mxCfg) {
        this.cfg = cfg;
        this.mxCfg = mxCfg;
    }

    protected LdapConfig getCfg() {
        return cfg;
    }

    protected List<String> getBaseDNs() {
        return cfg.getConnection().getBaseDNs();
    }

    protected LdapConfig.Attribute getAt() {
        return cfg.getAttribute();
    }

    protected String getUidAtt() {
        return getAt().getUid().getValue();
    }

    protected synchronized LdapConnection getConn() {
        return getConn(cfg.getConnection().getHost());
    }

    protected synchronized LdapConnection getConn(String host) {
        return new LdapNetworkConnection(host, cfg.getConnection().getPort(), cfg.getConnection().isTls());
    }

    protected void bind(LdapConnection conn) throws LdapException {
        if (StringUtils.isBlank(cfg.getConnection().getBindDn()) && StringUtils.isBlank(cfg.getConnection().getBindPassword())) {
            conn.anonymousBind();
        } else {
            conn.bind(cfg.getConnection().getBindDn(), cfg.getConnection().getBindPassword());
        }
    }

    protected String buildWithFilter(String base, String filter) {
        if (StringUtils.isBlank(filter)) {
            return base;
        } else {
            return "(&" + filter + base + ")";
        }
    }

    public static String buildOrQuery(String value, List<String> attributes) {
        if (attributes.size() < 1) {
            throw new IllegalArgumentException();
        }

        StringBuilder builder = new StringBuilder();
        builder.append("(|");
        attributes.forEach(s -> {
            builder.append("(");
            builder.append(s).append("=").append(value).append(")");
        });
        builder.append(")");
        return builder.toString();
    }

    public static String buildOrQuery(String value, String... attributes) {
        return buildOrQuery(value, Arrays.asList(attributes));
    }

    public String buildOrQueryWithFilter(String filter, String value, String... attributes) {
        return buildWithFilter(buildOrQuery(value, attributes), filter);
    }

    public String buildMatrixIdFromUid(String uid) {
        String uidType = getCfg().getAttribute().getUid().getType();
        if (StringUtils.equals(UID, uidType)) {
            return "@" + uid + ":" + mxCfg.getDomain();
        } else if (StringUtils.equals(MATRIX_ID, uidType)) {
            return uid;
        } else {
            throw new IllegalArgumentException("Bind type " + uidType + " is not supported");
        }
    }

    public String buildUidFromMatrixId(_MatrixID mxId) {
        String uidType = getCfg().getAttribute().getUid().getType();
        if (StringUtils.equals(UID, uidType)) {
            return mxId.getLocalPart();
        } else if (StringUtils.equals(MATRIX_ID, uidType)) {
            return mxId.getId();
        } else {
            throw new IllegalArgumentException("Bind type " + uidType + " is not supported");
        }
    }

    public Optional<String> getAttribute(Entry entry, String attName) {
        Attribute attribute = entry.get(attName);
        if (attribute == null) {
            return Optional.empty();
        }

        String value = attribute.get().toString();
        if (StringUtils.isBlank(value)) {
            log.info("DN {}: empty attribute {}, skipping", attName);
            return Optional.empty();
        }

        return Optional.of(value);
    }

    public List<String> getAttributes(Entry entry, String attName) {
        List<String> values = new ArrayList<>();
        javax.naming.directory.Attribute att = AttributeUtils.toAttributes(entry).get(attName);
        if (att == null) {
            return values;
        }

        try {
            NamingEnumeration<?> list = att.getAll();
            while (list.hasMore()) {
                values.add(list.next().toString());
            }
        } catch (NamingException e) {
            log.warn("Error while processing LDAP attribute {}, result could be incomplete!", attName, e);
        }
        return values;
    }

}
