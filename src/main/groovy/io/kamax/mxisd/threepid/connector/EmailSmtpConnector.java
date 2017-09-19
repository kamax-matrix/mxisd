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

package io.kamax.mxisd.threepid.connector;

import com.sun.mail.smtp.SMTPTransport;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.config.invite.medium.EmailInviteConfig;
import io.kamax.mxisd.config.threepid.connector.EmailSmtpConfig;
import io.kamax.mxisd.exception.ConfigurationException;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class EmailSmtpConnector implements IThreePidConnector {

    private Logger log = LoggerFactory.getLogger(EmailSmtpConnector.class);

    private EmailSmtpConfig cfg;
    private EmailInviteConfig invCfg;

    private Session session;
    private InternetAddress sender;

    @Autowired
    public EmailSmtpConnector(EmailSmtpConfig cfg, EmailInviteConfig invCfg) {
        try {
            session = Session.getInstance(System.getProperties());
            sender = new InternetAddress(invCfg.getFrom(), invCfg.getName());
        } catch (UnsupportedEncodingException e) {
            // What are we supposed to do with this?!
            throw new ConfigurationException(e);
        }

        this.cfg = cfg;
        this.invCfg = invCfg;
    }

    @Override
    public String getMedium() {
        return ThreePidMedium.Email.getId();
    }

    @Override
    public void send(IThreePidInviteReply invite, String content) {
        if (!ThreePidMedium.Email.is(invite.getInvite().getMedium())) {
            throw new IllegalArgumentException(invite.getInvite().getMedium() + " is not a supported 3PID type");
        }

        try {
            MimeMessage msg = new MimeMessage(session, IOUtils.toInputStream(content, StandardCharsets.UTF_8));
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
        } catch (MessagingException e) {
            throw new RuntimeException("Unable to send e-mail invite to " + invite.getInvite().getAddress(), e);
        }
    }

}
