package io.kamax.mxisd.test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.Mxisd;
import io.kamax.mxisd.as.MatrixIdInvite;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.config.threepid.connector.EmailSmtpConfig;
import io.kamax.mxisd.config.threepid.medium.EmailConfig;
import io.kamax.mxisd.threepid.connector.email.EmailSmtpConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Collections;

import static junit.framework.TestCase.assertEquals;

public class MxisdEmailNotifTest {

    private final String domain = "localhost";
    private Mxisd m;
    private GreenMail gm;

    @Before
    public void before() {
        EmailSmtpConfig smtpCfg = new EmailSmtpConfig();
        smtpCfg.setPort(3025);
        smtpCfg.setLogin("mxisd");
        smtpCfg.setPassword("mxisd");

        EmailConfig eCfg = new EmailConfig();
        eCfg.setConnector(EmailSmtpConnector.ID);
        eCfg.getIdentity().setFrom("mxisd@" + domain);
        eCfg.getIdentity().setName("Mxisd Server (Unit Test)");
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
        _MatrixID sender = MatrixID.asAcceptable("mxisd", domain);
        _MatrixID recipient = MatrixID.asAcceptable("john", domain);
        MatrixIdInvite idInvite = new MatrixIdInvite("!rid:" + domain, sender, recipient, ThreePidMedium.Email.getId(), "john@" + domain, Collections.emptyMap());
        m.getNotif().sendForInvite(idInvite);

        assertEquals(1, gm.getReceivedMessages().length);
        MimeMessage msg = gm.getReceivedMessages()[0];
        assertEquals(1, msg.getFrom().length);
        assertEquals("\"Mxisd Server (Unit Test)\" <mxisd@localhost>", msg.getFrom()[0].toString());
        assertEquals(1, msg.getRecipients(Message.RecipientType.TO).length);
    }

}
