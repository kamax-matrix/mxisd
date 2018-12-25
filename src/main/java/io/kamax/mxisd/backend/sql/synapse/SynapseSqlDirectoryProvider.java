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

package io.kamax.mxisd.backend.sql.synapse;

import io.kamax.mxisd.backend.sql.generic.GenericSqlDirectoryProvider;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.sql.generic.GenericSqlProviderConfig;
import io.kamax.mxisd.config.sql.synapse.SynapseSqlProviderConfig;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

public class SynapseSqlDirectoryProvider extends GenericSqlDirectoryProvider {

    public SynapseSqlDirectoryProvider(SynapseSqlProviderConfig cfg, MatrixConfig mxCfg) {
        super(cfg, mxCfg);

        GenericSqlProviderConfig.Type queries = cfg.getDirectory().getQuery();
        if (Objects.isNull(queries.getName().getValue())) {
            queries.getName().setValue(SynapseQueries.findByDisplayName(cfg.getType(), mxCfg.getDomain()));
        }
        if (Objects.isNull(queries.getThreepid().getValue())) {
            queries.getThreepid().setValue(SynapseQueries.findByThreePidAddress(cfg.getType(), mxCfg.getDomain()));
        }
    }

    @Override
    protected void setParameters(PreparedStatement stmt, String searchTerm) throws SQLException {
        stmt.setString(1, "%" + searchTerm + "%");
    }

}
