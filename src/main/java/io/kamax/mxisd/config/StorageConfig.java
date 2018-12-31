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

package io.kamax.mxisd.config;

import io.kamax.mxisd.exception.ConfigurationException;
import org.apache.commons.lang.StringUtils;

public class StorageConfig {

    public static class Provider {

        private SQLiteStorageConfig sqlite = new SQLiteStorageConfig();

        public SQLiteStorageConfig getSqlite() {
            return sqlite;
        }

        public void setSqlite(SQLiteStorageConfig sqlite) {
            this.sqlite = sqlite;
        }

    }

    private String backend = "sqlite";
    private Provider provider = new Provider();

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public void build() {
        if (StringUtils.isBlank(getBackend())) {
            throw new ConfigurationException("storage.backend");
        }
    }

}
