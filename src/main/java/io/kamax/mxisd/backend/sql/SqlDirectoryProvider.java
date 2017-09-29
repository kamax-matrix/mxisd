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

package io.kamax.mxisd.backend.sql;

import io.kamax.matrix.MatrixID;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.sql.SqlConfig;
import io.kamax.mxisd.config.sql.SqlProviderConfig;
import io.kamax.mxisd.controller.directory.io.UserDirectorySearchResult;
import io.kamax.mxisd.directory.IDirectoryProvider;
import io.kamax.mxisd.exception.InternalServerError;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static io.kamax.mxisd.controller.directory.io.UserDirectorySearchResult.Result;

public abstract class SqlDirectoryProvider implements IDirectoryProvider {

    private Logger log = LoggerFactory.getLogger(SqlDirectoryProvider.class);

    protected SqlConfig cfg;
    private MatrixConfig mxCfg;

    private SqlConnectionPool pool;

    public SqlDirectoryProvider(SqlConfig cfg, MatrixConfig mxCfg) {
        this.cfg = cfg;
        this.pool = new SqlConnectionPool(cfg);
        this.mxCfg = mxCfg;
    }

    @Override
    public boolean isEnabled() {
        return cfg.isEnabled();
    }

    protected void setParameters(PreparedStatement stmt, String searchTerm) throws SQLException {
        for (int i = 1; i <= stmt.getParameterMetaData().getParameterCount(); i++) {
            stmt.setString(i, searchTerm);
        }
    }

    protected Optional<Result> processRow(ResultSet rSet) throws SQLException {
        Result item = new Result();
        item.setUserId(rSet.getString(1));
        item.setDisplayName(rSet.getString(2));
        return Optional.of(item);
    }

    public UserDirectorySearchResult search(String searchTerm, SqlProviderConfig.Query query) {
        try (Connection conn = pool.get()) {
            log.info("Will execute query: {}", query.getValue());
            try (PreparedStatement stmt = conn.prepareStatement(query.getValue())) {
                setParameters(stmt, searchTerm);

                try (ResultSet rSet = stmt.executeQuery()) {
                    UserDirectorySearchResult result = new UserDirectorySearchResult();
                    result.setLimited(false);

                    while (rSet.next()) {
                        processRow(rSet).ifPresent(e -> {
                            if (StringUtils.equalsIgnoreCase("localpart", query.getType())) {
                                e.setUserId(new MatrixID(e.getUserId(), mxCfg.getDomain()).getId());
                            }
                            result.addResult(e);
                        });
                    }

                    return result;
                }
            }
        } catch (SQLException e) {
            throw new InternalServerError(e);
        }
    }

    @Override
    public UserDirectorySearchResult searchByDisplayName(String searchTerm) {
        log.info("Searching users by display name using '{}'", searchTerm);
        return search(searchTerm, cfg.getDirectory().getQuery().getName());
    }

    @Override
    public UserDirectorySearchResult searchBy3pid(String searchTerm) {
        log.info("Searching users by 3PID using '{}'", searchTerm);
        return search(searchTerm, cfg.getDirectory().getQuery().getThreepid());
    }

}
