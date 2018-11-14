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

package io.kamax.mxisd.config.threepid.medium;

import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.exception.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@ConfigurationProperties("threepid.medium.email")
public class EmailConfig {

    private Logger log = LoggerFactory.getLogger(EmailConfig.class);

    public static class Identity {
        private String from;
        private String name;

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    private String generator;
    private String connector;

    private MatrixConfig mxCfg;
    private Identity identity = new Identity();

    @Autowired
    public EmailConfig(MatrixConfig mxCfg) {
        this.mxCfg = mxCfg;
    }

    public Identity getIdentity() {
        return identity;
    }

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public String getConnector() {
        return connector;
    }

    public void setConnector(String connector) {
        this.connector = connector;
    }

    @PostConstruct
    public void build() {
        log.info("--- E-mail config ---");

        if (StringUtils.isBlank(getGenerator())) {
            throw new ConfigurationException("generator");
        }

        if (StringUtils.isBlank(getConnector())) {
            throw new ConfigurationException("connector");
        }

        log.info("From: {}", identity.getFrom());

        if (StringUtils.isBlank(identity.getName())) {
            identity.setName(WordUtils.capitalize(mxCfg.getDomain()) + " Identity Server");
        }
        log.info("Name: {}", identity.getName());
        log.info("Generator: {}", getGenerator());
        log.info("Connector: {}", getConnector());
    }

}
