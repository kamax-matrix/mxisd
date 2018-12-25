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

import io.kamax.matrix.ThreePid;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.auth.provider.BackendAuthResult;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordpressAuthProvider implements AuthenticatorProvider {

    private transient final Logger log = LoggerFactory.getLogger(WordpressAuthProvider.class);

    private WordpressRestBackend wordpress;

    public WordpressAuthProvider(WordpressRestBackend wordpress) {
        this.wordpress = wordpress;
    }

    @Override
    public boolean isEnabled() {
        return wordpress.isEnabled();
    }

    @Override
    public BackendAuthResult authenticate(_MatrixID mxid, String password) {
        try {
            WordpressAuthData data = wordpress.authenticate(mxid.getLocalPart(), password);
            BackendAuthResult result = new BackendAuthResult();
            if (StringUtils.isNotBlank(data.getUserEmail())) {
                result.withThreePid(new ThreePid("email", data.getUserEmail()));
            }
            result.succeed(mxid.getId(), UserIdType.MatrixID.getId(), data.getUserDisplayName());
            return result;
        } catch (IllegalArgumentException e) {
            log.error("Authentication failed for {}: {}", mxid.getId(), e.getMessage());
            return BackendAuthResult.failure();
        }

    }

}
