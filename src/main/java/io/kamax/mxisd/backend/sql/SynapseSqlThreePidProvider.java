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

import io.kamax.matrix.ThreePid;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.sql.synapse.SynapseSqlProviderConfig;
import io.kamax.mxisd.profile.ProfileWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

@Component
public class SynapseSqlThreePidProvider extends SqlThreePidProvider implements ProfileWriter {

    private final Logger log = LoggerFactory.getLogger(SynapseSqlThreePidProvider.class);

    @Autowired
    public SynapseSqlThreePidProvider(SynapseSqlProviderConfig cfg, MatrixConfig mxCfg) {
        super(cfg, mxCfg);
    }

    @Override
    public boolean addThreepid(_MatrixID mxid, ThreePid tpid) {
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO user_threepids (user_id, medium, address, validated_at, added_at) values (?,?,?,?,?)");
            stmt.setString(1, mxid.getId());
            stmt.setString(2, tpid.getMedium());
            stmt.setString(3, tpid.getAddress());
            stmt.setLong(4, Instant.now().toEpochMilli());
            stmt.setLong(5, Instant.now().toEpochMilli());

            int rows = stmt.executeUpdate();
            if (rows != 1) {
                log.error("Unable to update 3PID info. Modified row(s): {}", rows);
            }

            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
