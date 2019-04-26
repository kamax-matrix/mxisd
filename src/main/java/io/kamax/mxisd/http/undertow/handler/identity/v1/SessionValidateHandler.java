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

package io.kamax.mxisd.http.undertow.handler.identity.v1;

import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.session.SessionManager;
import io.kamax.mxisd.session.ValidationResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SessionValidateHandler extends BasicHttpHandler {

    public static final String Path = IsAPIv1.Base + "/validate/{medium}/submitToken";

    private transient final Logger log = LoggerFactory.getLogger(SessionValidateHandler.class);

    private SessionManager mgr;

    public SessionValidateHandler(SessionManager mgr) {
        this.mgr = mgr;
    }

    protected ValidationResult handleRequest(String sid, String secret, String token) {
        if (StringUtils.isEmpty(sid)) {
            throw new IllegalArgumentException("sid is not set or is empty");
        }

        if (StringUtils.isEmpty(secret)) {
            throw new IllegalArgumentException("client secret is not set or is empty");
        }

        return mgr.validate(sid, secret, token);
    }

}
