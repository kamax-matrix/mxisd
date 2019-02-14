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

package io.kamax.mxisd.backend.sql;

import io.kamax.matrix.ThreePid;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix._ThreePid;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.config.sql.SqlConfig;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.profile.ProfileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class SqlProfileProvider implements ProfileProvider {

    private static final Logger log = LoggerFactory.getLogger(SqlProfileProvider.class);

    private SqlConfig.Profile cfg;
    private SqlConnectionPool pool;

    public SqlProfileProvider(SqlConfig cfg) {
        this.cfg = cfg.getProfile();
        this.pool = new SqlConnectionPool(cfg);
    }

    private void setParameters(PreparedStatement stmt, String value) throws SQLException {
        for (int i = 1; i <= stmt.getParameterMetaData().getParameterCount(); i++) {
            stmt.setString(i, value);
        }
    }

    @Override
    public Optional<String> getDisplayName(_MatrixID user) {
        String stmtSql = cfg.getDisplayName().getQuery();
        try (Connection conn = pool.get()) {
            try (PreparedStatement stmt = conn.prepareStatement(stmtSql)) {
                stmt.setString(1, user.getId());

                try (ResultSet rSet = stmt.executeQuery()) {
                    if (!rSet.next()) {
                        return Optional.empty();
                    }

                    return Optional.ofNullable(rSet.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<_ThreePid> getThreepids(_MatrixID user) {
        List<_ThreePid> threepids = new ArrayList<>();

        String stmtSql = cfg.getThreepid().getQuery();
        try (Connection conn = pool.get()) {
            PreparedStatement stmt = conn.prepareStatement(stmtSql);
            stmt.setString(1, user.getId());

            ResultSet rSet = stmt.executeQuery();
            while (rSet.next()) {
                String medium = rSet.getString(1);
                String address = rSet.getString(2);
                threepids.add(new ThreePid(medium, address));
            }

            return threepids;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getRoles(_MatrixID user) {
        log.info("Querying roles for {}", user.getId());

        List<String> roles = new ArrayList<>();

        String stmtSql = cfg.getRole().getQuery();
        try (Connection conn = pool.get()) {
            PreparedStatement stmt = conn.prepareStatement(stmtSql);
            if (UserIdType.Localpart.is(cfg.getRole().getType())) {
                setParameters(stmt, user.getLocalPart());
            } else if (UserIdType.MatrixID.is(cfg.getRole().getType())) {
                setParameters(stmt, user.getId());
            } else {
                throw new InternalServerError("Unsupported user type in SQL Role fetching: " + cfg.getRole().getType());
            }

            ResultSet rSet = stmt.executeQuery();
            while (rSet.next()) {
                String role = rSet.getString(1);
                roles.add(role);
                log.debug("Found role {}", role);
            }

            log.info("Got {} roles", roles.size());
            return roles;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
