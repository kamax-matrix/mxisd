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

package io.kamax.mxisd.lookup.provider;

import io.kamax.matrix.MatrixID;
import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.config.sql.SqlProviderConfig;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SqlProvider implements IThreePidProvider {

    private Logger log = LoggerFactory.getLogger(SqlProvider.class);

    @Autowired
    private ServerConfig srvCfg;

    @Autowired
    private SqlProviderConfig cfg;

    @Override
    public boolean isEnabled() {
        return cfg.isEnabled();
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public int getPriority() {
        return 20;
    }

    private Connection getConn() throws SQLException {
        return DriverManager.getConnection("jdbc:" + cfg.getType() + ":" + cfg.getConnection());
    }

    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        log.info("SQL lookup");
        String stmtSql = StringUtils.defaultIfBlank(cfg.getIdentity().getMedium().get(request.getType()), cfg.getIdentity().getQuery());
        log.info("SQL query: {}", stmtSql);
        try (PreparedStatement stmt = getConn().prepareStatement(stmtSql)) {
            stmt.setString(1, request.getType().toLowerCase());
            stmt.setString(2, request.getThreePid().toLowerCase());

            ResultSet rSet = stmt.executeQuery();
            while (rSet.next()) {
                String uid = rSet.getString("uid");
                log.info("Found match: {}", uid);
                if (StringUtils.equals("uid", cfg.getIdentity().getType())) {
                    log.info("Resolving as localpart");
                    return Optional.of(new SingleLookupReply(request, new MatrixID(uid, srvCfg.getName())));
                }
                if (StringUtils.equals("mxid", cfg.getIdentity().getType())) {
                    log.info("Resolving as MXID");
                    return Optional.of(new SingleLookupReply(request, new MatrixID(uid)));
                }

                log.info("Identity type is unknown, skipping");
            }

            log.info("No match found in SQL");
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        return new ArrayList<>();
    }

}
