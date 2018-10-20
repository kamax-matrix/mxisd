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

import io.kamax.mxisd.exception.NotImplementedException;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.provider.IThreePidProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ExecIdentityStore extends ExecStore implements IThreePidProvider {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        throw new NotImplementedException(this.getClass().getName());
    }

    @Override
    public List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        throw new NotImplementedException(this.getClass().getName());
    }

}
