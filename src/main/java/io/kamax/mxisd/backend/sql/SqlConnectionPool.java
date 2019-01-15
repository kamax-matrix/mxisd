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

package io.kamax.mxisd.backend.sql;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.kamax.mxisd.config.sql.SqlConfig;

import java.sql.Connection;
import java.sql.SQLException;

public class SqlConnectionPool {

    public interface SqlFunction<T, R> {

        R run(T connection) throws SQLException;

    }

    private ComboPooledDataSource ds;

    public SqlConnectionPool(SqlConfig cfg) {
        Drivers.load(cfg.getType());

        ds = new ComboPooledDataSource();
        ds.setJdbcUrl("jdbc:" + cfg.getType() + ":" + cfg.getConnection());
        ds.setMinPoolSize(1);
        ds.setMaxPoolSize(10);
        ds.setAcquireIncrement(2);
        ds.setAcquireRetryAttempts(10);
        ds.setAcquireRetryDelay(1000);
    }

    public Connection get() throws SQLException {
        return ds.getConnection();
    }

    public <T> T withConnFunction(SqlFunction<Connection, T> function) {
        try (Connection conn = get()) {
            return function.run(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
