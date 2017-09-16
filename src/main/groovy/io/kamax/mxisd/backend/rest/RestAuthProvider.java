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

package io.kamax.mxisd.backend.rest;

import io.kamax.mxisd.auth.UserAuthResult;
import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.config.rest.RestBackendConfig;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;

public class RestAuthProvider implements AuthenticatorProvider {

    @Autowired
    private RestBackendConfig cfg;

    @Override
    public boolean isEnabled() {
        return cfg.isEnabled();
    }

    @Override
    public UserAuthResult authenticate(String id, String password) {
        throw new NotImplementedException();
    }

}
