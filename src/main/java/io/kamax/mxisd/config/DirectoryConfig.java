/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Kamax Sarl
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoryConfig {

    private final static Logger log = LoggerFactory.getLogger(DirectoryConfig.class);

    public static class Exclude {

        private boolean homeserver;
        private boolean threepid;

        public boolean getHomeserver() {
            return homeserver;
        }

        public void setHomeserver(boolean homeserver) {
            this.homeserver = homeserver;
        }

        public boolean getThreepid() {
            return threepid;
        }

        public void setThreepid(boolean threepid) {
            this.threepid = threepid;
        }

    }

    private Exclude exclude = new Exclude();

    public Exclude getExclude() {
        return exclude;
    }

    public void setExclude(Exclude exclude) {
        this.exclude = exclude;
    }

    public void build() {
        log.info("--- Directory config ---");
        log.info("Exclude:");
        log.info("  Homeserver: {}", getExclude().getHomeserver());
        log.info("  3PID: {}", getExclude().getThreepid());
    }

}
