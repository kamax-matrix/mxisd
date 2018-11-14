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

package io.kamax.mxisd.storage.ormlite;

import io.kamax.mxisd.config.SQLiteStorageConfig;
import io.kamax.mxisd.config.StorageConfig;
import io.kamax.mxisd.exception.ConfigurationException;
import io.kamax.mxisd.storage.IStorage;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class OrmLiteSqliteStorageBeanFactory implements FactoryBean<IStorage> {

    @Autowired
    private StorageConfig storagecfg;

    @Autowired
    private SQLiteStorageConfig cfg;

    private OrmLiteSqliteStorage storage;

    @PostConstruct
    private void postConstruct() {
        if (StringUtils.equals("sqlite", storagecfg.getBackend())) {
            if (StringUtils.isBlank(cfg.getDatabase())) {
                throw new ConfigurationException("storage.provider.sqlite.database");
            }

            storage = new OrmLiteSqliteStorage(cfg.getDatabase());
        }
    }

    @Override
    public IStorage getObject() throws Exception {
        if (storage == null) {
            throw new FactoryBeanNotInitializedException();
        }

        return storage;
    }

    @Override
    public Class<?> getObjectType() {
        return OrmLiteSqliteStorage.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
