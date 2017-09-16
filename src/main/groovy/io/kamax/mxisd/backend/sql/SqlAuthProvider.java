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

import io.kamax.mxisd.auth.UserAuthResult;
import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.config.sql.SqlProviderConfig;
import io.kamax.mxisd.invitation.InvitationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SqlAuthProvider implements AuthenticatorProvider {

    private Logger log = LoggerFactory.getLogger(SqlAuthProvider.class);

    @Autowired
    private ServerConfig srvCfg;

    @Autowired
    private SqlProviderConfig cfg;

    @Autowired
    private InvitationManager invMgr;

    @Override
    public boolean isEnabled() {
        return cfg.isEnabled();
    }

    @Override
    public UserAuthResult authenticate(String id, String password) {
        log.info("Performing dummy authentication try to force invite mapping refresh");

        invMgr.lookupMappingsForInvites();
        return new UserAuthResult().failure();
    }

}
