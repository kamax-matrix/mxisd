/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
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

package io.kamax.mxisd.backend.ldap.netiq;

import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.backend.ldap.LdapProfileProvider;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.ldap.netiq.NetIqLdapConfig;

public class NetIqLdapProfileProvider extends LdapProfileProvider {

    public NetIqLdapProfileProvider(NetIqLdapConfig cfg, MatrixConfig mxCfg) {
        super(cfg, mxCfg);
    }

    // FIXME this is duplicated in the other NetIQ classes, due to the Matrix ID generation code that was not abstracted
    @Override
    public String buildMatrixIdFromUid(String uid) {
        return super.buildMatrixIdFromUid(uid).toLowerCase();
    }

    // FIXME this is duplicated in the other NetIQ classes, due to the Matrix ID generation code that was not abstracted
    @Override
    public String buildUidFromMatrixId(_MatrixID mxid) {
        return super.buildUidFromMatrixId(mxid).toLowerCase();
    }

}
