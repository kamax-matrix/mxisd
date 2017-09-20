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

package io.kamax.mxisd.config.threepid.medium;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@ConfigurationProperties("threepid.medium.email.generators.template")
public class EmailTemplateConfig {

    private static Logger log = LoggerFactory.getLogger(EmailTemplateConfig.class);
    private static final String classpathPrefix = "classpath:";

    private static String getName(String path) {
        if (StringUtils.startsWith(path, classpathPrefix)) {
            return "Built-in (" + path.substring(classpathPrefix.length()) + ")";
        }

        return path;
    }

    public static class Session {

        private String validation;

        public String getValidation() {
            return validation;
        }

        public void setValidation(String validation) {
            this.validation = validation;
        }

    }

    private String invite;
    private Session session = new Session();

    public String getInvite() {
        return invite;
    }

    public void setInvite(String invite) {
        this.invite = invite;
    }

    public Session getSession() {
        return session;
    }

    @PostConstruct
    public void build() {
        log.info("--- E-mail Generator templates config ---");
        log.info("Invite: {}", getName(getInvite()));
        log.info("Session validation: {}", getName(getSession().getValidation()));
    }

}
