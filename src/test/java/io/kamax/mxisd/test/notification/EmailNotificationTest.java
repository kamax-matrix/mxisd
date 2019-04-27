/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2019 Kamax Sarl
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

package io.kamax.mxisd.test.notification;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix.ThreePid;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.Mxisd;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.config.threepid.connector.EmailSmtpConfig;
import io.kamax.mxisd.config.threepid.medium.EmailConfig;
import io.kamax.mxisd.invitation.MatrixIdInvite;
import io.kamax.mxisd.invitation.ThreePidInvite;
import io.kamax.mxisd.invitation.ThreePidInviteReply;
import io.kamax.mxisd.threepid.connector.email.EmailSmtpConnector;
import io.kamax.mxisd.threepid.generator.PlaceholderNotificationGenerator;
import io.kamax.mxisd.threepid.session.ThreePidSession;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class EmailNotificationTest {

    private final String domain = "localhost";
    private final String user = "mxisd";
    private final String notifiee = "john";
    private final String sender = user + "@" + domain;
    private final String senderName = "\"Mxisd Server あ (Unit Test)\" <" + sender + ">";
    private final String senderNameEncoded = "=?UTF-8?Q?=22Mxisd_Server_=E3=81=82_=28Unit_T?= =?UTF-8?Q?est=29=22_=3Cmxisd=40localhost=3E?= <mxisd@localhost>";
    private final String target = notifiee + "@" + domain;

    private Mxisd m;
    private GreenMail gm;

    @Before
    public void before() {
        EmailSmtpConfig smtpCfg = new EmailSmtpConfig();
        smtpCfg.setPort(3025);
        smtpCfg.setLogin(user);
        smtpCfg.setPassword(user);

        EmailConfig eCfg = new EmailConfig();
        eCfg.setConnector(EmailSmtpConnector.ID);
        eCfg.getIdentity().setFrom(sender);
        eCfg.getIdentity().setName(senderName);
        eCfg.getConnectors().put(EmailSmtpConnector.ID, GsonUtil.makeObj(smtpCfg));

        MxisdConfig cfg = new MxisdConfig();
        cfg.getMatrix().setDomain(domain);
        cfg.getKey().setPath(":memory:");
        cfg.getStorage().getProvider().getSqlite().setDatabase(":memory:");
        cfg.getThreepid().getMedium().put(ThreePidMedium.Email.getId(), GsonUtil.makeObj(eCfg));

        m = new Mxisd(cfg);
        m.start();

        gm = new GreenMail(ServerSetupTest.SMTP_IMAP);
        gm.start();
    }

    @After
    public void after() {
        gm.stop();
        m.stop();
    }

    @Test
    public void forMatrixIdInvite() throws MessagingException {
        gm.setUser("mxisd", "mxisd");

        _MatrixID sender = MatrixID.asAcceptable(user, domain);
        _MatrixID recipient = MatrixID.asAcceptable(notifiee, domain);
        MatrixIdInvite idInvite = new MatrixIdInvite(
                "!rid:" + domain,
                sender,
                recipient,
                ThreePidMedium.Email.getId(),
                target,
                Collections.emptyMap()
        );

        m.getNotif().sendForInvite(idInvite);

        assertEquals(1, gm.getReceivedMessages().length);
        MimeMessage msg = gm.getReceivedMessages()[0];
        assertEquals(1, msg.getFrom().length);
        assertEquals(senderNameEncoded, msg.getFrom()[0].toString());
        assertEquals(1, msg.getRecipients(Message.RecipientType.TO).length);
    }

    @Test
    public void forThreepidInvite() throws MessagingException, IOException {
        String registerUrl = "https://" + RandomStringUtils.randomAlphanumeric(20) + ".example.org/register";
        gm.setUser(user, user);

        _MatrixID sender = MatrixID.asAcceptable(user, domain);
        ThreePidInvite inv = new ThreePidInvite(sender, ThreePidMedium.Email.getId(), target, "!rid:" + domain);
        inv.getProperties().put(PlaceholderNotificationGenerator.RegisterUrl, registerUrl);
        m.getNotif().sendForReply(new ThreePidInviteReply("a", inv, "b", "c", new ArrayList<>()));

        assertEquals(1, gm.getReceivedMessages().length);
        MimeMessage msg = gm.getReceivedMessages()[0];
        assertEquals(1, msg.getFrom().length);
        assertEquals(senderNameEncoded, msg.getFrom()[0].toString());
        assertEquals(1, msg.getRecipients(Message.RecipientType.TO).length);

        // We just check on the text/plain one. HTML is multipart and it's difficult so we skip
        MimeMultipart content = (MimeMultipart) msg.getContent();
        MimeBodyPart mbp = (MimeBodyPart) content.getBodyPart(0);
        String mbpContent = mbp.getContent().toString();
        assertTrue(mbpContent.contains(registerUrl));
    }

    @Test
    public void forValidation() throws MessagingException, IOException {
        gm.setUser(user, user);

        String token = RandomStringUtils.randomAlphanumeric(128);
        ThreePidSession session = new ThreePidSession(
                "",
                "",
                new ThreePid(ThreePidMedium.Email.getId(), target),
                "",
                1,
                "",
                token
        );

        m.getNotif().sendForValidation(session);

        assertEquals(1, gm.getReceivedMessages().length);
        MimeMessage msg = gm.getReceivedMessages()[0];
        assertEquals(1, msg.getFrom().length);
        assertEquals(senderNameEncoded, msg.getFrom()[0].toString());
        assertEquals(1, msg.getRecipients(Message.RecipientType.TO).length);

        // We just check on the text/plain one. HTML is multipart and it's difficult so we skip
        MimeMultipart content = (MimeMultipart) msg.getContent();
        MimeBodyPart mbp = (MimeBodyPart) content.getBodyPart(0);
        String mbpContent = mbp.getContent().toString();
        assertTrue(mbpContent.contains(token));
    }

}
