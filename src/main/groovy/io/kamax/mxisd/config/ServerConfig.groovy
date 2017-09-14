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

package io.kamax.mxisd.config

import io.kamax.mxisd.exception.ConfigurationException
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "server")
class ServerConfig implements InitializingBean {

    private Logger log = LoggerFactory.getLogger(ServerConfig.class);

    private String name
    private int port
    private String publicUrl

    String getName() {
        return name
    }

    void setName(String name) {
        this.name = name
    }

    int getPort() {
        return port
    }

    void setPort(int port) {
        this.port = port
    }

    String getPublicUrl() {
        return publicUrl
    }

    void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl
    }

    @Override
    void afterPropertiesSet() throws Exception {
        if (StringUtils.isBlank(getName())) {
            throw new ConfigurationException("server.name")
        }

        if (StringUtils.isBlank(getPublicUrl())) {
            log.warn("Public URL is empty, generating from name {}", getName())
            publicUrl = "https://${getName()}"
        }

        try {
            new URL(getPublicUrl())
        } catch (MalformedURLException e) {
            log.warn("Public URL is not valid: {}", StringUtils.defaultIfBlank(e.getMessage(), "<no reason provided>"))
        }

        log.info("--- Server config ---")
        log.info("Name: {}", getName())
        log.info("Port: {}", getPort())
        log.info("Public URL: {}", getPublicUrl())
    }

}
