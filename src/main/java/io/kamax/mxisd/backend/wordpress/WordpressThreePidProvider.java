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
import io.kamax.matrix.ThreePid;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.wordpress.WordpressConfig;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.provider.IThreePidProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class WordpressThreePidProvider implements IThreePidProvider {

    private transient final Logger log = LoggerFactory.getLogger(WordpressThreePidProvider.class);

    private MatrixConfig mxCfg;
    private WordpressConfig cfg;
    private WordressSqlBackend wordpress;

    public WordpressThreePidProvider(MatrixConfig mxCfg, WordpressConfig cfg, WordressSqlBackend wordpress) {
        this.mxCfg = mxCfg;
        this.cfg = cfg;
        this.wordpress = wordpress;
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public int getPriority() {
        return 15;
    }

    protected Optional<_MatrixID> find(ThreePid tpid) {
        String query = cfg.getSql().getQuery().getThreepid().get(tpid.getMedium());
        if (Objects.isNull(query)) {
            return Optional.empty();
        }

        try (Connection conn = wordpress.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, tpid.getAddress());

            try (ResultSet rSet = stmt.executeQuery()) {
                while (rSet.next()) {
                    String uid = rSet.getString("uid");
                    log.info("Found match: {}", uid);
                    try {
                        return Optional.of(MatrixID.from(uid, mxCfg.getDomain()).valid());
                    } catch (IllegalArgumentException ex) {
                        log.warn("Ignoring match {} - Invalid characters for a Matrix ID", uid);
                    }
                }

                log.info("No valid match found in Wordpress");
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        return find(new ThreePid(request.getType(), request.getThreePid())).map(mxid -> new SingleLookupReply(request, mxid));
    }

    @Override
    public List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        for (ThreePidMapping tpidMap : mappings) {
            find(new ThreePid(tpidMap.getMedium(), tpidMap.getValue())).ifPresent(mxid -> tpidMap.setMxid(mxid.getId()));
        }
        return mappings;
    }

}
