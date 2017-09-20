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

package io.kamax.mxisd.invitation.generator;

import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.invite.medium.EmailInviteConfig;
import io.kamax.mxisd.config.threepid.medium.EmailConfig;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class EmailInviteContentGenerator implements IInviteContentGenerator {

    private EmailConfig cfg;
    private EmailInviteConfig invCfg;
    private MatrixConfig mxCfg;
    private ApplicationContext app;

    @Autowired // FIXME ApplicationContext shouldn't be injected, find another way from config (?)
    public EmailInviteContentGenerator(EmailConfig cfg, EmailInviteConfig invCfg, MatrixConfig mxCfg, ApplicationContext app) {
        this.cfg = cfg;
        this.invCfg = invCfg;
        this.mxCfg = mxCfg;
        this.app = app;
    }

    @Override
    public String getMedium() {
        return ThreePidMedium.Email.getId();
    }

    @Override
    public String generate(IThreePidInviteReply invite) {
        if (!ThreePidMedium.Email.is(invite.getInvite().getMedium())) {
            throw new IllegalArgumentException(invite.getInvite().getMedium() + " is not a supported 3PID type");
        }

        try {
            String domainPretty = WordUtils.capitalizeFully(mxCfg.getDomain());
            String senderName = invite.getInvite().getProperties().getOrDefault("sender_display_name", "");
            String senderNameOrId = StringUtils.defaultIfBlank(senderName, invite.getInvite().getSender().getId());
            String roomName = invite.getInvite().getProperties().getOrDefault("room_name", "");
            String roomNameOrId = StringUtils.defaultIfBlank(roomName, invite.getInvite().getRoomId());

            String templateBody = IOUtils.toString(
                    StringUtils.startsWith(invCfg.getTemplate(), "classpath:") ?
                            app.getResource(invCfg.getTemplate()).getInputStream() : new FileInputStream(invCfg.getTemplate()),
                    StandardCharsets.UTF_8);
            templateBody = templateBody.replace("%DOMAIN%", mxCfg.getDomain());
            templateBody = templateBody.replace("%DOMAIN_PRETTY%", domainPretty);
            templateBody = templateBody.replace("%FROM_EMAIL%", cfg.getFrom());
            templateBody = templateBody.replace("%FROM_NAME%", cfg.getName());
            templateBody = templateBody.replace("%SENDER_ID%", invite.getInvite().getSender().getId());
            templateBody = templateBody.replace("%SENDER_NAME%", senderName);
            templateBody = templateBody.replace("%SENDER_NAME_OR_ID%", senderNameOrId);
            templateBody = templateBody.replace("%INVITE_MEDIUM%", invite.getInvite().getMedium());
            templateBody = templateBody.replace("%INVITE_ADDRESS%", invite.getInvite().getAddress());
            templateBody = templateBody.replace("%ROOM_ID%", invite.getInvite().getRoomId());
            templateBody = templateBody.replace("%ROOM_NAME%", roomName);
            templateBody = templateBody.replace("%ROOM_NAME_OR_ID%", roomNameOrId);

            return templateBody;
        } catch (IOException e) {
            throw new RuntimeException("Unable to read template file", e);
        }
    }

}
