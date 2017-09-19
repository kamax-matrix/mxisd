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

package io.kamax.mxisd.config.invite.medium;

import io.kamax.mxisd.config.MatrixConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration
@ConfigurationProperties("invite.medium.email")
public class EmailInviteConfig {

    private Logger log = LoggerFactory.getLogger(EmailInviteConfig.class);

    private MatrixConfig mxCfg;

    private String from;
    private String name;
    private String template;

    @Autowired
    public EmailInviteConfig(MatrixConfig mxCfg) {
        this.mxCfg = mxCfg;
    }

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

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    @PostConstruct
    public void build() {
        log.info("--- E-mail invites config ---");
        log.info("From: {}", getFrom());

        if (StringUtils.isBlank(getName())) {
            setName(WordUtils.capitalize(mxCfg.getDomain()) + " Identity Server");
        }
        log.info("Name: {}", getName());

        if (!StringUtils.startsWith(getTemplate(), "classpath:")) {
            if (StringUtils.isBlank(getTemplate())) {
                log.warn("invite.medium.email is empty! Will not send invites");
            } else {
                File cp = new File(getTemplate()).getAbsoluteFile();
                log.info("Template: {}", cp.getAbsolutePath());
                if (!cp.exists() || !cp.isFile() || !cp.canRead()) {
                    log.warn(getTemplate() + " does not exist, is not a file or cannot be read");
                }
            }
        } else {
            log.info("Template: Built-in: {}", getTemplate());
        }
    }

}
