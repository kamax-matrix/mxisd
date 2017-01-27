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

import groovy.json.JsonSlurper
import io.kamax.mxisd.api.ThreePidType
import org.springframework.stereotype.Component

@Component
class RootProvider implements ThreePidProvider {

    private List<String> roots = Arrays.asList("https://matrix.org", "https://vector.im")
    private JsonSlurper json = new JsonSlurper()

    @Override
    int getPriority() {
        return 0
    }

    @Override
    Optional<?> find(ThreePidType type, String threePid) {
        for (String root : roots) {
            HttpURLConnection rootSrvConn = (HttpURLConnection) new URL(
                    "${root}/_matrix/identity/api/v1/lookup?medium=${type}&address=${threePid}"
            ).openConnection()

            def output = json.parseText(rootSrvConn.getInputStream().getText())
            if (output['address'] != null) {
                return Optional.of(output)
            }
        }

        return Optional.empty()
    }

}
