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

import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.sql.GenericSqlProviderConfig;
import io.kamax.mxisd.config.sql.synapse.SynapseSqlProviderConfig;
import io.kamax.mxisd.exception.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@Component
public class SynapseSqlDirectoryProvider extends GenericSqlDirectoryProvider {

    @Autowired
    public SynapseSqlDirectoryProvider(SynapseSqlProviderConfig cfg, MatrixConfig mxCfg) {
        super(cfg, mxCfg);

        if (StringUtils.equals("sqlite", cfg.getType())) {
            String userId = "'@' || p.user_id || ':" + mxCfg.getDomain() + "'";
            GenericSqlProviderConfig.Type queries = cfg.getDirectory().getQuery();
            queries.getName().setValue(
                    "select " + userId + ", displayname from profiles p where displayname like ?");
            queries.getThreepid().setValue(
                    "select t.user_id, p.displayname " +
                            "from user_threepids t JOIN profiles p on t.user_id = " + userId + " " +
                            "where t.address like ?");
        } else if (StringUtils.equals("postgresql", cfg.getType())) {
            String userId = "concat('@',p.user_id,':" + mxCfg.getDomain() + "')";
            GenericSqlProviderConfig.Type queries = cfg.getDirectory().getQuery();
            queries.getName().setValue(
                    "select " + userId + ", displayname from profiles p where displayname ilike ?");
            queries.getThreepid().setValue(
                    "select t.user_id, p.displayname " +
                            "from user_threepids t JOIN profiles p on t.user_id = " + userId + " " +
                            "where t.address ilike ?");
        } else {
            throw new ConfigurationException("Invalid SQL type");
        }
    }

    @Override
    protected void setParameters(PreparedStatement stmt, String searchTerm) throws SQLException {
        stmt.setString(1, "%" + searchTerm + "%");
    }

}
