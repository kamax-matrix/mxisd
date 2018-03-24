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

package io.kamax.mxisd.profile;

import io.kamax.matrix._MatrixID;
import io.kamax.matrix._ThreePid;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProfileManager {

    private List<ProfileProvider> providers;

    public ProfileManager(List<ProfileProvider> providers) {
        this.providers = providers.stream()
                .filter(ProfileProvider::isEnabled)
                .collect(Collectors.toList());
    }

    public <T> List<T> get(Function<ProfileProvider, List<T>> function) {
        return providers.stream()
                .map(function)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<_ThreePid> getThreepids(_MatrixID mxid) {
        return get(p -> p.getThreepids(mxid));
    }

    public List<String> getRoles(_MatrixID mxid) {
        return get(p -> p.getRoles(mxid));
    }

}
