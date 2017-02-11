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

package io.kamax.mxisd.lookup

import io.kamax.mxisd.api.ThreePidType
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PriorityLookupStrategy implements LookupStrategy, InitializingBean {

    @Autowired
    private List<ThreePidProvider> providers

    @Override
    void afterPropertiesSet() throws Exception {
        providers.sort(new Comparator<ThreePidProvider>() {

            @Override
            int compare(ThreePidProvider o1, ThreePidProvider o2) {
                return Integer.compare(o2.getPriority(), o1.getPriority())
            }

        })
    }

    @Override
    Optional<?> find(ThreePidType type, String threePid) {
        if (ThreePidType.email != type) {
            throw new IllegalArgumentException("${type} is currently not supported")
        }

        for (ThreePidProvider provider : providers) {
            Optional<?> lookupDataOpt = provider.find(type, threePid)
            if (lookupDataOpt.isPresent()) {
                return lookupDataOpt
            }
        }

        return Optional.empty()
    }

}
