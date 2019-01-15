/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2019 Kamax Sàrl
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

import org.apache.commons.lang3.StringUtils;

public class BuiltInDriverLoader implements DriverLoader {

    @Override
    public void accept(String s) {
        String className = null;
        if (StringUtils.equals("sqlite", s)) {
            className = "org.sqlite.JDBC";
        }

        if (StringUtils.equals("postgresql", s)) {
            className = "org.postgresql.Driver";
        }

        if (StringUtils.equals("mariadb", s)) {
            className = "org.mariadb.jdbc.Driver";
        }

        if (StringUtils.equals("mysql", s)) {
            className = "org.mariadb.jdbc.Driver";
        }

        if (StringUtils.isNotEmpty(className)) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
