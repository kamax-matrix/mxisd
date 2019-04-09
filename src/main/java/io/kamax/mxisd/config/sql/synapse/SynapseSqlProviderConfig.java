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

package io.kamax.mxisd.config.sql.synapse;

import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.backend.sql.synapse.SynapseQueries;
import io.kamax.mxisd.config.sql.SqlConfig;
import org.apache.commons.lang.StringUtils;

public class SynapseSqlProviderConfig extends SqlConfig {

    @Override
    protected String getProviderName() {
        return "Synapse SQL";
    }

    public void build() {
        super.build();

        getAuth().setEnabled(false); // Synapse does the auth, we only act as a directory/identity service.

        // FIXME check that the DB is not the mxisd one
        // See https://matrix.to/#/!NPRUEisLjcaMtHIzDr:kamax.io/$1509377583327omXkC:kamax.io

        if (getIdentity().isEnabled() && StringUtils.isBlank(getIdentity().getType())) {
            getIdentity().setType("mxid");
            getIdentity().setQuery("SELECT user_id AS uid FROM user_threepids WHERE medium = ? AND address = ?");
        }

        if (getProfile().isEnabled()) {
            if (StringUtils.isBlank(getProfile().getDisplayName().getQuery())) {
                getProfile().getDisplayName().setQuery(SynapseQueries.getDisplayName());
            }

            if (StringUtils.isBlank(getProfile().getThreepid().getQuery())) {
                getProfile().getThreepid().setQuery(SynapseQueries.getThreepids());
            }

            if (StringUtils.isBlank(getProfile().getRole().getType())) {
                getProfile().getRole().setType(UserIdType.MatrixID.getId());
            }
            if (StringUtils.isBlank(getProfile().getRole().getQuery())) {
                getProfile().getRole().setQuery(SynapseQueries.getRoles());
            }
        }

        printConfig();
    }

}
