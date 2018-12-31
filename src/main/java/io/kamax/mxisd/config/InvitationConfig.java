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

package io.kamax.mxisd.config;

import io.kamax.mxisd.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvitationConfig {

    private transient final Logger log = LoggerFactory.getLogger(InvitationConfig.class);

    public static class Resolution {

        private boolean recursive = true;
        private long timer = 1;

        public boolean isRecursive() {
            return recursive;
        }

        public void setRecursive(boolean recursive) {
            this.recursive = recursive;
        }

        public long getTimer() {
            return timer;
        }

        public void setTimer(long timer) {
            this.timer = timer;
        }

    }

    private Resolution resolution = new Resolution();

    public Resolution getResolution() {
        return resolution;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
    }

    public void build() {
        log.info("--- Invite config ---");
        log.info("Resolution: {}", GsonUtil.build().toJson(resolution));
    }

}
