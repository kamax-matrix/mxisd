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

import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.kamax.mxisd.config.wordpress.WordpressConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class WordressSqlBackend {

    private transient final Logger log = LoggerFactory.getLogger(WordressSqlBackend.class);

    private WordpressConfig cfg;
    private ComboPooledDataSource ds;

    public WordressSqlBackend(WordpressConfig cfg) {
        this.cfg = cfg;

        ds = new ComboPooledDataSource();
        ds.setJdbcUrl("jdbc:" + cfg.getSql().getType() + ":" + cfg.getSql().getConnection());
        ds.setMinPoolSize(1);
        ds.setMaxPoolSize(10);
        ds.setAcquireIncrement(2);
    }

    public boolean isEnabled() {
        return cfg.isEnabled();
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

}
