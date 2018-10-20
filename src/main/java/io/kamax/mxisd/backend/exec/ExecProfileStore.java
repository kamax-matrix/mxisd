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

import io.kamax.matrix._MatrixID;
import io.kamax.matrix._ThreePid;
import io.kamax.mxisd.exception.NotImplementedException;
import io.kamax.mxisd.profile.ProfileProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ExecProfileStore extends ExecStore implements ProfileProvider {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public Optional<String> getDisplayName(_MatrixID userId) {
        throw new NotImplementedException(this.getClass().getName());
    }

    @Override
    public List<_ThreePid> getThreepids(_MatrixID userId) {
        throw new NotImplementedException(this.getClass().getName());
    }

    @Override
    public List<String> getRoles(_MatrixID userId) {
        throw new NotImplementedException(this.getClass().getName());
    }

}
