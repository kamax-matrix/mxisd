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

package io.kamax.mxisd.session;

import io.kamax.mxisd.threepid.session.IThreePidSession;

import java.util.Optional;

public class ValidationResult {

    private IThreePidSession session;
    private boolean canRemote;
    private String nextUrl;

    public ValidationResult(IThreePidSession session, boolean canRemote) {
        this.session = session;
        this.canRemote = canRemote;
    }

    public IThreePidSession getSession() {
        return session;
    }

    public boolean isCanRemote() {
        return canRemote;
    }

    public Optional<String> getNextUrl() {
        return Optional.ofNullable(nextUrl);
    }

    public void setNextUrl(String nextUrl) {
        this.nextUrl = nextUrl;
    }

}
