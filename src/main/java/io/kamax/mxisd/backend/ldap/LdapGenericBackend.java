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
import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

public abstract class LdapGenericBackend {

    public static final String UID = "uid";
    public static final String MATRIX_ID = "mxid";

    private Logger log = LoggerFactory.getLogger(LdapGenericBackend.class);

    private LdapConfig cfg;
    private MatrixConfig mxCfg;

    public LdapGenericBackend(LdapConfig cfg, MatrixConfig mxCfg) {
        this.cfg = cfg;
        this.mxCfg = mxCfg;
    }

    protected LdapConfig getCfg() {
        return cfg;
    }

    protected String getBaseDn() {
        return cfg.getConn().getBaseDn();
    }

    protected LdapAttributeConfig getAt() {
        return cfg.getAttribute();
    }

    protected String getUidAtt() {
        return getAt().getUid().getValue();
    }

    protected LdapConnection getConn() {
        return new LdapNetworkConnection(cfg.getConn().getHost(), cfg.getConn().getPort(), cfg.getConn().isTls());
    }

    protected void bind(LdapConnection conn) throws LdapException {
        if (StringUtils.isBlank(cfg.getConn().getBindDn()) && StringUtils.isBlank(cfg.getConn().getBindPassword())) {
            conn.anonymousBind();
        } else {
            conn.bind(cfg.getConn().getBindDn(), cfg.getConn().getBindPassword());
        }
    }

    protected String buildWithFilter(String base, String filter) {
        if (StringUtils.isBlank(filter)) {
            return base;
        } else {
            return "(&" + filter + base + ")";
        }
    }

    public static String buildOrQuery(String value, String... attributes) {
        StringBuilder builder = new StringBuilder();
        builder.append("(|");
        Arrays.stream(attributes).forEach(s -> {
            builder.append("(");
            builder.append(s).append("=").append(value).append(")");
        });
        builder.append(")");
        return builder.toString();
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

    public Optional<String> getAttribute(Entry entry, String attName) {
        Attribute attribute = entry.get(attName);
        if (attribute == null) {
            log.info("DN {}: no attribute {}, skipping", entry.getDn(), attName);
            return Optional.empty();
        }

        String value = attribute.get().toString();
        if (StringUtils.isBlank(value)) {
            log.info("DN {}: empty attribute {}, skipping", attName);
            return Optional.empty();
        }

        return Optional.of(value);
    }

}
