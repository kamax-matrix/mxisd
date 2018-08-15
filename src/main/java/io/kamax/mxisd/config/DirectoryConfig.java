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

package io.kamax.mxisd.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@ConfigurationProperties("directory")
public class DirectoryConfig {

    private final transient Logger log = LoggerFactory.getLogger(DnsOverwriteConfig.class);

    public static class Exclude {

        private boolean homeserver;
        private boolean threepid;

        public boolean getHomeserver() {
            return homeserver;
        }

        public Exclude setHomeserver(boolean homeserver) {
            this.homeserver = homeserver;
            return this;
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

    @PostConstruct
    public void buid() {
        log.info("--- Directory config ---");
        log.info("Exclude:");
        log.info("\tHomeserver: {}", getExclude().getHomeserver());
        log.info("\t3PID: {}", getExclude().getThreepid());
    }

}
