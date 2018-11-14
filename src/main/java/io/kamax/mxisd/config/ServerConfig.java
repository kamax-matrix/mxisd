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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;

@Configuration
@ConfigurationProperties(prefix = "server")
public class ServerConfig {

    private Logger log = LoggerFactory.getLogger(ServerConfig.class);

    @Autowired
    private MatrixConfig mxCfg;

    private String name;
    private int port;
    private String publicUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    @PostConstruct
    public void build() {
        log.info("--- Server config ---");

        if (StringUtils.isBlank(getName())) {
            setName(mxCfg.getDomain());
            log.debug("server.name is empty, using matrix.domain");
        }

        if (StringUtils.isBlank(getPublicUrl())) {
            setPublicUrl("https://" + getName());
            log.debug("Public URL is empty, generating from name");
        } else {
            setPublicUrl(StringUtils.replace(getPublicUrl(), "%SERVER_NAME%", getName()));
        }

        try {
            new URL(getPublicUrl());
        } catch (MalformedURLException e) {
            log.warn("Public URL is not valid: {}", StringUtils.defaultIfBlank(e.getMessage(), "<no reason provided>"));
        }

        log.info("Name: {}", getName());
        log.info("Port: {}", getPort());
        log.info("Public URL: {}", getPublicUrl());
    }

}
