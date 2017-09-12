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

import io.kamax.mxisd.ThreePid;
import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
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
    private InvitationManager invMgr;

    public UserAuthResult authenticate(String id, String password) {
        for (AuthenticatorProvider provider : providers) {
            if (!provider.isEnabled()) {
                continue;
            }

            UserAuthResult result = provider.authenticate(id, password);
            if (result.isSuccess()) {
                log.info("{} was authenticated by {}, publishing 3PID mappings, if any", id, provider.getClass().getSimpleName());
                for (ThreePid pid : result.getThreePids()) {
                    log.info("Processing {} for {}", pid, id);
                    invMgr.publishMappingIfInvited(new ThreePidMapping(pid, result.getMxid()));
                }

                return result;
            }
        }

        return new UserAuthResult().failure();
    }

}
