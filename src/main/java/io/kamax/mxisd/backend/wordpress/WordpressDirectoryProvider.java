/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sàrl
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

package io.kamax.mxisd.backend.wordpress;

import io.kamax.matrix.MatrixID;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.wordpress.WordpressConfig;
import io.kamax.mxisd.directory.DirectoryProvider;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.http.io.UserDirectorySearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class WordpressDirectoryProvider implements DirectoryProvider {

    private transient final Logger log = LoggerFactory.getLogger(WordpressDirectoryProvider.class);

    private WordpressConfig cfg;
    private WordressSqlBackend wordpress;
    private MatrixConfig mxCfg;

    public WordpressDirectoryProvider(WordpressConfig cfg, WordressSqlBackend wordpress, MatrixConfig mxCfg) {
        this.cfg = cfg;
        this.wordpress = wordpress;
        this.mxCfg = mxCfg;
    }

    protected void setParameters(PreparedStatement stmt, String searchTerm) throws SQLException {
        for (int i = 1; i <= stmt.getParameterMetaData().getParameterCount(); i++) {
            stmt.setString(i, "%" + searchTerm + "%");
        }
    }

    protected Optional<UserDirectorySearchResult.Result> processRow(ResultSet rSet) throws SQLException {
        UserDirectorySearchResult.Result item = new UserDirectorySearchResult.Result();
        item.setUserId(rSet.getString(1));
        item.setDisplayName(rSet.getString(2));
        return Optional.of(item);
    }

    public UserDirectorySearchResult search(String searchTerm, String query) {
        try (Connection conn = wordpress.getConnection()) {
            log.info("Will execute query: {}", query);
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                setParameters(stmt, searchTerm);

                try (ResultSet rSet = stmt.executeQuery()) {
                    UserDirectorySearchResult result = new UserDirectorySearchResult();
                    result.setLimited(false);

                    while (rSet.next()) {
                        processRow(rSet).ifPresent(e -> {
                            try {
                                e.setUserId(MatrixID.from(e.getUserId(), mxCfg.getDomain()).valid().getId());
                                result.addResult(e);
                            } catch (IllegalArgumentException ex) {
                                log.warn("Ignoring result {} - Invalid characters for a Matrix ID", e.getUserId());
                            }
                        });
                    }

                    return result;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new InternalServerError(e);
        }
    }

    @Override
    public UserDirectorySearchResult searchByDisplayName(String searchTerm) {
        log.info("Searching users by display name using '{}'", searchTerm);
        return search(searchTerm, cfg.getSql().getQuery().getDirectory().get("name"));
    }

    @Override
    public UserDirectorySearchResult searchBy3pid(String searchTerm) {
        log.info("Searching users by 3PID using '{}'", searchTerm);
        return search(searchTerm, cfg.getSql().getQuery().getDirectory().get("threepid"));
    }

}
