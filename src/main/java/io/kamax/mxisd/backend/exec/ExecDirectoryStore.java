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

package io.kamax.mxisd.backend.exec;

import io.kamax.mxisd.controller.directory.v1.io.UserDirectorySearchResult;
import io.kamax.mxisd.directory.IDirectoryProvider;
import io.kamax.mxisd.exception.NotImplementedException;
import org.springframework.stereotype.Component;

@Component
public class ExecDirectoryStore extends ExecStore implements IDirectoryProvider {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public UserDirectorySearchResult searchByDisplayName(String query) {
        throw new NotImplementedException(this.getClass().getName());
    }

    @Override
    public UserDirectorySearchResult searchBy3pid(String query) {
        throw new NotImplementedException(this.getClass().getName());
    }

}
