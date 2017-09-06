package io.kamax.mxisd.invitation.sender;

import com.sun.mail.smtp.SMTPTransport;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.config.invite.sender.EmailSenderConfig;
import io.kamax.mxisd.exception.ConfigurationException;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Date;

@Component
public class EmailInviteSender implements IInviteSender {

    private Logger log = LoggerFactory.getLogger(EmailInviteSender.class);

    @Autowired
    private EmailSenderConfig cfg;

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
            MimeMessage msg = new MimeMessage(session, new FileInputStream(cfg.getContentPath()));
            msg.setHeader("X-Mailer", "mxisd");
            msg.setSentDate(new Date());
            msg.setRecipients(Message.RecipientType.TO, invite.getInvite().getAddress());
            msg.setFrom(sender);

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
