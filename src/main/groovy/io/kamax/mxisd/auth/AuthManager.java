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

package io.kamax.mxisd.auth;

import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.ThreePid;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.auth.provider.BackendAuthResult;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.invitation.InvitationManager;
import io.kamax.mxisd.lookup.ThreePidMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthManager {

    private Logger log = LoggerFactory.getLogger(AuthManager.class);

    @Autowired
    private List<AuthenticatorProvider> providers = new ArrayList<>();

    @Autowired
    private MatrixConfig mxCfg;

    @Autowired
    private InvitationManager invMgr;

    public UserAuthResult authenticate(String id, String password) {
        _MatrixID mxid = new MatrixID(id);
        for (AuthenticatorProvider provider : providers) {
            if (!provider.isEnabled()) {
                continue;
            }

            BackendAuthResult result = provider.authenticate(mxid, password);
            if (result.isSuccess()) {

                String mxId;
                if (UserIdType.Localpart.is(result.getId().getType())) {
                    mxId = new MatrixID(result.getId().getValue(), mxCfg.getDomain()).getId();
                } else if (UserIdType.MatrixID.is(result.getId().getType())) {
                    mxId = new MatrixID(result.getId().getValue()).getId();
                } else {
                    log.warn("Unsupported User ID type {} for backend {}", result.getId().getType(), provider.getClass().getSimpleName());
                    continue;
                }

                UserAuthResult authResult = new UserAuthResult().success(mxId, result.getProfile().getDisplayName());
                for (ThreePid pid : result.getProfile().getThreePids()) {
                    authResult.withThreePid(pid.getMedium(), pid.getAddress());
                }
                log.info("{} was authenticated by {}, publishing 3PID mappings, if any", id, provider.getClass().getSimpleName());
                for (ThreePid pid : authResult.getThreePids()) {
                    log.info("Processing {} for {}", pid, id);
                    invMgr.publishMappingIfInvited(new ThreePidMapping(pid, authResult.getMxid()));
                }

                invMgr.lookupMappingsForInvites();

                return authResult;
            }
        }

        return new UserAuthResult().failure();
    }

}
