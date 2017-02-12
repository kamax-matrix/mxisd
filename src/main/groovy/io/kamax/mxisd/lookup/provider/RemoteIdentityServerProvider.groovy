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

package io.kamax.mxisd.lookup.provider

import groovy.json.JsonException
import groovy.json.JsonSlurper
import io.kamax.mxisd.api.ThreePidType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class RemoteIdentityServerProvider implements ThreePidProvider {

    private Logger log = LoggerFactory.getLogger(RemoteIdentityServerProvider.class)

    private JsonSlurper json = new JsonSlurper()

    @Override
    boolean isLocal() {
        return false
    }

    Optional<?> find(String remote, ThreePidType type, String threePid) {
        log.info("Looking up {} 3PID {} using {}", type, threePid, remote)

        HttpURLConnection rootSrvConn = (HttpURLConnection) new URL(
                "${remote}/_matrix/identity/api/v1/lookup?medium=${type}&address=${threePid}"
        ).openConnection()

        try {
            def output = json.parseText(rootSrvConn.getInputStream().getText())
            if (output['address']) {
                log.info("Found 3PID mapping: {}", output)
                return Optional.of(output)
            }

            log.info("Empty 3PID mapping from {}", remote)
            return Optional.empty()
        } catch (IOException e) {
            log.warn("Error looking up 3PID mapping {}: {}", threePid, e.getMessage())
            return Optional.empty()
        } catch (JsonException e) {
            log.warn("Invalid JSON answer from {}", remote)
            return Optional.empty()
        }
    }

}
