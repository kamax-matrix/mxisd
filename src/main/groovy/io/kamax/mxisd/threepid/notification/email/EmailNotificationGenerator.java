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

package io.kamax.mxisd.threepid.notification.email;

import io.kamax.mxisd.ThreePid;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.config.threepid.medium.EmailConfig;
import io.kamax.mxisd.config.threepid.medium.EmailTemplateConfig;
import io.kamax.mxisd.controller.v1.IdentityAPIv1;
import io.kamax.mxisd.controller.v1.remote.RemoteIdentityAPIv1;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import io.kamax.mxisd.threepid.session.IThreePidSession;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class EmailNotificationGenerator implements IEmailNotificationGenerator {

    private Logger log = LoggerFactory.getLogger(EmailNotificationGenerator.class);

    private EmailConfig cfg;
    private EmailTemplateConfig templateCfg;
    private MatrixConfig mxCfg;
    private ServerConfig srvCfg;

    @Autowired
    private ApplicationContext app;

    @Autowired
    public EmailNotificationGenerator(EmailTemplateConfig templateCfg, EmailConfig cfg, MatrixConfig mxCfg, ServerConfig srvCfg) {
        this.cfg = cfg;
        this.templateCfg = templateCfg;
        this.mxCfg = mxCfg;
        this.srvCfg = srvCfg;
    }

    @Override
    public String getId() {
        return "template";
    }

    private String getTemplateContent(String location) {
        try {
            InputStream is = StringUtils.startsWith(location, "classpath:") ?
                    app.getResource(location).getInputStream() : new FileInputStream(location);
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new InternalServerError("Unable to read template content at " + location + ": " + e.getMessage());
        }
    }

    private String populateCommon(String content, ThreePid recipient) {
        String domainPretty = WordUtils.capitalizeFully(mxCfg.getDomain());

        content = content.replace("%DOMAIN%", mxCfg.getDomain());
        content = content.replace("%DOMAIN_PRETTY%", domainPretty);
        content = content.replace("%FROM_EMAIL%", cfg.getIdentity().getFrom());
        content = content.replace("%FROM_NAME%", cfg.getIdentity().getName());
        content = content.replace("%RECIPIENT_MEDIUM%", recipient.getMedium());
        content = content.replace("%RECIPIENT_ADDRESS%", recipient.getAddress());
        return content;
    }

    private String getTemplateAndPopulate(String location, ThreePid recipient) {
        return populateCommon(getTemplateContent(location), recipient);
    }

    @Override
    public String getForInvite(IThreePidInviteReply invite) {
        ThreePid tpid = new ThreePid(invite.getInvite().getMedium(), invite.getInvite().getAddress());
        String templateBody = getTemplateAndPopulate(templateCfg.getInvite(), tpid);

        String senderName = invite.getInvite().getProperties().getOrDefault("sender_display_name", "");
        String senderNameOrId = StringUtils.defaultIfBlank(senderName, invite.getInvite().getSender().getId());
        String roomName = invite.getInvite().getProperties().getOrDefault("room_name", "");
        String roomNameOrId = StringUtils.defaultIfBlank(roomName, invite.getInvite().getRoomId());

        templateBody = templateBody.replace("%SENDER_ID%", invite.getInvite().getSender().getId());
        templateBody = templateBody.replace("%SENDER_NAME%", senderName);
        templateBody = templateBody.replace("%SENDER_NAME_OR_ID%", senderNameOrId);
        templateBody = templateBody.replace("%INVITE_MEDIUM%", tpid.getMedium());
        templateBody = templateBody.replace("%INVITE_ADDRESS%", tpid.getAddress());
        templateBody = templateBody.replace("%ROOM_ID%", invite.getInvite().getRoomId());
        templateBody = templateBody.replace("%ROOM_NAME%", roomName);
        templateBody = templateBody.replace("%ROOM_NAME_OR_ID%", roomNameOrId);

        return templateBody;
    }

    @Override
    public String getForValidation(IThreePidSession session) {
        log.info("Generating notification content for 3PID Session validation");
        String templateBody = getTemplateAndPopulate(templateCfg.getSession().getValidation().getLocal(), session.getThreePid());

        // FIXME should have a global link builder, most likely in the SDK?
        String validationLink = srvCfg.getPublicUrl() + IdentityAPIv1.BASE +
                "/validate/" + session.getThreePid().getMedium() +
                "/submitToken?sid=" + session.getId() + "&client_secret=" + session.getSecret() +
                "&token=" + session.getToken();

        templateBody = templateBody.replace("%VALIDATION_LINK%", validationLink);
        templateBody = templateBody.replace("%VALIDATION_TOKEN%", session.getToken());

        return templateBody;
    }

    @Override
    public String getForRemoteValidation(IThreePidSession session) {
        log.info("Generating notification content for remote-only 3PID session");
        String templateBody = getTemplateAndPopulate(templateCfg.getSession().getValidation().getRemote(), session.getThreePid());

        // FIXME should have a global link builder, specific to mxisd
        String nextStepLink = srvCfg.getPublicUrl() + RemoteIdentityAPIv1.BASE +
                "/validate/requestToken?sid=" + session.getId() +
                "&client_secret=" + session.getSecret() +
                "&token=" + session.getToken();

        templateBody = templateBody.replace("%SESSION_ID%", session.getId());
        templateBody = templateBody.replace("%SESSION_SECRET%", session.getSecret());
        templateBody = templateBody.replace("%SESSION_TOKEN%", session.getToken());
        templateBody = templateBody.replace("%NEXT_STEP_LINK%", nextStepLink);

        return templateBody;
    }

}
