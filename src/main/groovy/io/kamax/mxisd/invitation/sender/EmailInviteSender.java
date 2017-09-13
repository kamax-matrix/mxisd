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

package io.kamax.mxisd.invitation.sender;

import com.sun.mail.smtp.SMTPTransport;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.config.invite.sender.EmailSenderConfig;
import io.kamax.mxisd.exception.ConfigurationException;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class EmailInviteSender implements IInviteSender {

    private Logger log = LoggerFactory.getLogger(EmailInviteSender.class);

    @Autowired
    private EmailSenderConfig cfg;

    @Autowired
    private ServerConfig srvCfg;

    @Autowired
    private ApplicationContext app;

    private Session session;
    private InternetAddress sender;

    @PostConstruct
    private void postConstruct() {
        try {
            session = Session.getInstance(System.getProperties());
            sender = new InternetAddress(cfg.getEmail(), cfg.getName());
        } catch (UnsupportedEncodingException e) {
            // What are we supposed to do with this?!
            throw new ConfigurationException(e);
        }
    }

    @Override
    public String getMedium() {
        return ThreePidMedium.Email.getId();
    }

    @Override
    public void send(IThreePidInviteReply invite) {
        if (!ThreePidMedium.Email.is(invite.getInvite().getMedium())) {
            throw new IllegalArgumentException(invite.getInvite().getMedium() + " is not a supported 3PID type");
        }

        try {
            String domainPretty = WordUtils.capitalizeFully(srvCfg.getName());
            String senderName = invite.getInvite().getProperties().getOrDefault("sender_display_name", "");
            String senderNameOrId = StringUtils.defaultIfBlank(senderName, invite.getInvite().getSender().getId());
            String roomName = invite.getInvite().getProperties().getOrDefault("room_name", "");
            String roomNameOrId = StringUtils.defaultIfBlank(roomName, invite.getInvite().getRoomId());

            String templateBody = IOUtils.toString(
                    StringUtils.startsWith(cfg.getTemplate(), "classpath:") ?
                            app.getResource(cfg.getTemplate()).getInputStream() : new FileInputStream(cfg.getTemplate()),
                    StandardCharsets.UTF_8);
            templateBody = templateBody.replace("%DOMAIN%", srvCfg.getName());
            templateBody = templateBody.replace("%DOMAIN_PRETTY%", domainPretty);
            templateBody = templateBody.replace("%FROM_EMAIL%", cfg.getEmail());
            templateBody = templateBody.replace("%FROM_NAME%", cfg.getName());
            templateBody = templateBody.replace("%SENDER_ID%", invite.getInvite().getSender().getId());
            templateBody = templateBody.replace("%SENDER_NAME%", senderName);
            templateBody = templateBody.replace("%SENDER_NAME_OR_ID%", senderNameOrId);
            templateBody = templateBody.replace("%ROOM_ID%", invite.getInvite().getRoomId());
            templateBody = templateBody.replace("%ROOM_NAME%", roomName);
            templateBody = templateBody.replace("%ROOM_NAME_OR_ID%", roomNameOrId);

            MimeMessage msg = new MimeMessage(session, IOUtils.toInputStream(templateBody, StandardCharsets.UTF_8));
            msg.setHeader("X-Mailer", "mxisd"); // TODO set version
            msg.setSentDate(new Date());
            msg.setFrom(sender);
            msg.setRecipients(Message.RecipientType.TO, invite.getInvite().getAddress());
            msg.saveChanges();

            log.info("Sending invite to {} via SMTP using {}:{}", invite.getInvite().getAddress(), cfg.getHost(), cfg.getPort());
            SMTPTransport transport = (SMTPTransport) session.getTransport("smtp");
            transport.setStartTLS(cfg.getTls() > 0);
            transport.setRequireStartTLS(cfg.getTls() > 1);

            log.info("Connecting to {}:{}", cfg.getHost(), cfg.getPort());
            transport.connect(cfg.getHost(), cfg.getPort(), cfg.getLogin(), cfg.getPassword());
            try {
                transport.sendMessage(msg, InternetAddress.parse(invite.getInvite().getAddress()));
                log.info("Invite to {} was sent", invite.getInvite().getAddress());
            } finally {
                transport.close();
            }
        } catch (IOException | MessagingException e) {
            throw new RuntimeException("Unable to send e-mail invite to " + invite.getInvite().getAddress(), e);
        }
    }

}
