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

package io.kamax.mxisd.threepid.connector.email;

import com.sun.mail.smtp.SMTPTransport;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.config.threepid.connector.EmailSmtpConfig;
import io.kamax.mxisd.exception.FeatureNotAvailable;
import io.kamax.mxisd.exception.InternalServerError;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Properties;

@Component
public class EmailSmtpConnector implements IEmailConnector {

    private Logger log = LoggerFactory.getLogger(EmailSmtpConnector.class);

    private EmailSmtpConfig cfg;
    private Session session;

    @Autowired
    public EmailSmtpConnector(EmailSmtpConfig cfg) {
        this.cfg = cfg;

        Properties sCfg = new Properties();
        sCfg.setProperty("mail.smtp.host", cfg.getHost());
        sCfg.setProperty("mail.smtp.port", Integer.toString(cfg.getPort()));

        // This seems very fiddly as we need to call different connect() methods depending
        // If there is authentication or not. We set those for the sake of completeness and
        // Backward compatibility. See previously opened issues about Email and SMTP.
        if (StringUtils.isAllEmpty(cfg.getLogin(), cfg.getPassword())) {
            sCfg.setProperty("mail.smtp.auth", "false");
        } else {
            sCfg.setProperty("mail.smtp.auth", "true");
        }

        session = Session.getInstance(sCfg);
    }

    @Override
    public String getId() {
        return "smtp";
    }

    @Override
    public String getMedium() {
        return ThreePidMedium.Email.getId();
    }

    @Override
    public void send(String senderAddress, String senderName, String recipient, String content) {
        if (StringUtils.isBlank(senderAddress)) {
            throw new FeatureNotAvailable("3PID Email identity: sender address is empty - " +
                    "You must set a value for notifications to work");
        }

        if (StringUtils.isBlank(content)) {
            throw new InternalServerError("Notification content is empty");
        }

        try {
            InternetAddress sender = new InternetAddress(senderAddress, senderName);
            MimeMessage msg = new MimeMessage(session, IOUtils.toInputStream(content, StandardCharsets.UTF_8));
            msg.setHeader("X-Mailer", "mxisd"); // FIXME set version
            msg.setSentDate(new Date());
            msg.setFrom(sender);
            msg.setRecipients(Message.RecipientType.TO, recipient);
            msg.saveChanges();

            log.info("Sending invite to {} via SMTP using {}:{}", recipient, cfg.getHost(), cfg.getPort());
            SMTPTransport transport = (SMTPTransport) session.getTransport("smtp");
            transport.setStartTLS(cfg.getTls() > 0);
            transport.setRequireStartTLS(cfg.getTls() > 1);

            log.info("Connecting to {}:{}", cfg.getHost(), cfg.getPort());
            if (StringUtils.isAllEmpty(cfg.getLogin(), cfg.getPassword())) {
                log.info("Not using SMTP authentication");
                transport.connect();
            } else {
                log.info("Using SMTP authentication");
                transport.connect(cfg.getLogin(), cfg.getPassword());
            }

            try {
                transport.sendMessage(msg, InternetAddress.parse(recipient));
                log.info("Invite to {} was sent", recipient);
            } finally {
                transport.close();
            }
        } catch (UnsupportedEncodingException | MessagingException e) {
            throw new RuntimeException("Unable to send e-mail invite to " + recipient, e);
        }
    }

}
