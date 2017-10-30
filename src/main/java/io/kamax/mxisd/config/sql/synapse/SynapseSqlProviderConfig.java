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

package io.kamax.mxisd.config.sql.synapse;

import io.kamax.mxisd.config.sql.SqlConfig;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@ConfigurationProperties("synapseSql")
public class SynapseSqlProviderConfig extends SqlConfig {

    @Override
    protected String getProviderName() {
        return "Synapse SQL";
    }

    @PostConstruct
    public void doBuild() {
        super.doBuild();
        // FIXME check that the DB is not the mxisd one
        // See https://matrix.to/#/!NPRUEisLjcaMtHIzDr:kamax.io/$1509377583327omXkC:kamax.io

        getAuth().setEnabled(false); // Synapse does the auth, we only act as a directory/identity service.

        if (getDirectory().isEnabled()) {
            //FIXME set default queries for name and threepid
        }

        if (getIdentity().isEnabled()) {
            if (StringUtils.isBlank(getIdentity().getType())) {
                getIdentity().setType("mxid");
                getIdentity().setQuery("SELECT user_id AS uid FROM user_threepids WHERE medium = ? AND address = ?");
            }
        }
    }

}
