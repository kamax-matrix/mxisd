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

package io.kamax.mxisd.threepid.generator;

import io.kamax.mxisd.as.IMatrixIdInvite;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.config.threepid.medium.GenericTemplateConfig;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import io.kamax.mxisd.threepid.session.IThreePidSession;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public abstract class GenericTemplateNotificationGenerator extends PlaceholderNotificationGenerator implements NotificationGenerator {

    private transient final Logger log = LoggerFactory.getLogger(GenericTemplateNotificationGenerator.class);

    private GenericTemplateConfig cfg;

    public GenericTemplateNotificationGenerator(MatrixConfig mxCfg, ServerConfig srvCfg, GenericTemplateConfig cfg) {
        super(mxCfg, srvCfg);
        this.cfg = cfg;
    }

    private String getTemplateContent(String location) {
        try {
            URI loc = URI.create(location);
            InputStream is;
            if (StringUtils.equals("classpath", loc.getScheme())) {
                is = getClass().getResourceAsStream(loc.getSchemeSpecificPart());
            } else {
                is = new FileInputStream(location);
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new InternalServerError("Unable to read template content at " + location + ": " + e.getMessage());
        }
    }

    @Override
    public String getForInvite(IMatrixIdInvite invite) {
        String template = cfg.getGeneric().get("matrixId");
        if (StringUtils.isBlank(template)) {
            throw new InternalServerError("No " + invite.getMedium() + " template configured for Matrix ID invites");
        }

        log.info("Generating notification content for Matrix ID invite");
        return populateForInvite(invite, getTemplateContent(template));
    }

    @Override
    public String getForReply(IThreePidInviteReply invite) {
        log.info("Generating notification content for 3PID invite");
        return populateForReply(invite, getTemplateContent(cfg.getInvite()));
    }

    @Override
    public String getForValidation(IThreePidSession session) {
        log.info("Generating notification content for 3PID Session validation");
        return populateForValidation(session, getTemplateContent(cfg.getSession().getValidation().getLocal()));
    }

    @Override
    public String getForRemoteValidation(IThreePidSession session) {
        log.info("Generating notification content for remote-only 3PID session");
        return populateForRemoteValidation(session, getTemplateContent(cfg.getSession().getValidation().getRemote()));
    }

}
