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

package io.kamax.mxisd.backend.sql.generic;

import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.auth.provider.BackendAuthResult;
import io.kamax.mxisd.config.sql.generic.GenericSqlProviderConfig;
import io.kamax.mxisd.invitation.InvitationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericSqlAuthProvider implements AuthenticatorProvider {

    private transient final Logger log = LoggerFactory.getLogger(GenericSqlAuthProvider.class);

    private GenericSqlProviderConfig cfg;
    private InvitationManager invMgr;

    public GenericSqlAuthProvider(GenericSqlProviderConfig cfg, InvitationManager invMgr) {
        this.cfg = cfg;
        this.invMgr = invMgr;
    }

    @Override
    public boolean isEnabled() {
        return cfg.getAuth().isEnabled();
    }

    @Override
    public BackendAuthResult authenticate(_MatrixID mxid, String password) {
        log.info("Performing dummy authentication try to force invite mapping refresh");

        invMgr.lookupMappingsForInvites();
        return BackendAuthResult.failure();
    }

}
