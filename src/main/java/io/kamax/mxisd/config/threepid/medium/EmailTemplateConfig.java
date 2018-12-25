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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

public class EmailTemplateConfig extends GenericTemplateConfig {

    private transient final Logger log = LoggerFactory.getLogger(EmailTemplateConfig.class);

    public EmailTemplateConfig() {
        setInvite("classpath:threepids/email/invite-template.eml");
        getGeneric().put("matrixId", "classpath:threepids/email/mxid-template.eml");
        getSession().getValidation().setLocal("classpath:threepids/email/validate-local-template.eml");
        getSession().getValidation().setRemote("classpath:threepids/email/validate-remote-template.eml");
    }

    @PostConstruct
    public void build() {
        log.info("--- E-mail Generator templates config ---");
        log.info("Invite: {}", getName(getInvite()));
        log.info("Session validation:");
        log.info("\tLocal: {}", getName(getSession().getValidation().getLocal()));
        log.info("\tRemote: {}", getName(getSession().getValidation().getRemote()));
    }

}
