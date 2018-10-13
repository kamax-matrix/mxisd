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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProfileManager {

    private final Logger log = LoggerFactory.getLogger(ProfileManager.class);

    private List<ProfileProvider> providers;

    @Autowired
    public ProfileManager(List<ProfileProvider> providers) {
        this.providers = providers;
    }

    @PostConstruct
    public void build() {
        log.info("--- Profile providers ---");
        providers = providers.stream()
                .filter(pp -> {
                    log.info("\t- {} - Is enabled? {}", pp.getClass().getSimpleName(), pp.isEnabled());
                    return pp.isEnabled();
                })
                .collect(Collectors.toList());
    }

    public <T> List<T> getList(Function<ProfileProvider, List<T>> function) {
        return providers.stream()
                .map(function)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public <T> Optional<T> getOpt(Function<ProfileProvider, Optional<T>> function) {
        return providers.stream()
                .map(function)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    public Optional<String> getDisplayName(_MatrixID user) {
        return getOpt(p -> p.getDisplayName(user));
    }

    public List<_ThreePid> getThreepids(_MatrixID user) {
        return getList(p -> p.getThreepids(user));
    }

    public List<String> getRoles(_MatrixID user) {
        return getList(p -> p.getRoles(user));
    }

}
