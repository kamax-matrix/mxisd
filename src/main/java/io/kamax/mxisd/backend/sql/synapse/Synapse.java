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

package io.kamax.mxisd.backend.sql.synapse;

import io.kamax.mxisd.backend.sql.SqlConnectionPool;
import io.kamax.mxisd.config.sql.synapse.SynapseSqlProviderConfig;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class Synapse {

    private SqlConnectionPool pool;

    public Synapse(SynapseSqlProviderConfig sqlCfg) {
        this.pool = new SqlConnectionPool(sqlCfg);
    }

    public Optional<String> getRoomName(String id) {
        return pool.withConnFunction(conn -> {
            PreparedStatement stmt = conn.prepareStatement(SynapseQueries.getRoomName());
            stmt.setString(1, id);
            ResultSet rSet = stmt.executeQuery();
            if (!rSet.next()) {
                return Optional.empty();
            }

            return Optional.ofNullable(rSet.getString(1));
        });
    }

}
