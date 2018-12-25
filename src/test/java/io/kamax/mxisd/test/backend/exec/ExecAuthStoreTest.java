/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
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

package io.kamax.mxisd.test.backend.exec;

import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.backend.exec.ExecAuthResult;
import io.kamax.mxisd.backend.exec.ExecAuthStore;
import io.kamax.mxisd.config.ExecConfig;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public abstract class ExecAuthStoreTest {

    protected final ExecConfig cfg;
    protected final ExecAuthStore p;
    protected final String requiredPass = Long.toString(System.currentTimeMillis());
    protected final String localpart = "user";
    protected final String domain = "domain.tld";
    protected final _MatrixID uId = MatrixID.from(localpart, domain).valid();

    protected final String LocalpartToken = "{localpart}";
    protected final String DomainToken = "{domain}";
    protected final String MxidToken = "{mxid}";
    protected final String PassToken = "{password}";

    protected final String LocalpartInvalid = "@:";
    protected final String DomainInvalid = "[.]:";
    protected final String MxidInvalid = LocalpartInvalid + DomainInvalid;
    protected final String PassInvalid = RandomStringUtils.randomAscii(20);

    protected abstract void setValidCommand();

    protected void setValidEnv() {
        // no-op
    }

    protected void setValidArgs() {
        // no-op
    }

    protected void setValidInput() {
        // no-op
    }

    protected void setValidExit() {
        cfg.getAuth().getExit().setSuccess(Collections.singletonList(0));
        cfg.getAuth().getExit().setFailure(Arrays.asList(1, 10, 11, 12, 20, 21, 22));
    }

    @Before
    public void setValidConfig() {
        setValidCommand();
        setValidEnv();
        setValidArgs();
        setValidInput();
        setValidExit();

        cfg.getAuth().addEnv("WITH_LOCALPART", "1");
        cfg.getAuth().addEnv("REQ_LOCALPART", uId.getLocalPart());
        cfg.getAuth().addEnv("WITH_DOMAIN", "1");
        cfg.getAuth().addEnv("REQ_DOMAIN", uId.getDomain());
        cfg.getAuth().addEnv("WITH_MXID", "1");
        cfg.getAuth().addEnv("REQ_MXID", uId.getId());
        cfg.getAuth().addEnv("REQ_PASS", requiredPass);
    }

    public ExecAuthStoreTest() {
        cfg = new ExecConfig();
        p = new ExecAuthStore(cfg);
    }

    @Test
    public void validPassword() {
        ExecAuthResult res = p.authenticate(uId, requiredPass);
        assertEquals(true, res.isSuccess());
        assertEquals(0, res.getExitStatus());
        assertEquals(UserIdType.Localpart.getId(), res.getId().getType());
        assertEquals(uId.getLocalPart(), res.getId().getValue());
    }

    @Test
    public void invalidPassword() {
        ExecAuthResult res = p.authenticate(uId, PassInvalid);
        assertEquals(false, res.isSuccess());
        assertEquals(1, res.getExitStatus());
    }

    @Test
    public void emptyPassword() {
        ExecAuthResult res = p.authenticate(uId, "");
        assertEquals(false, res.isSuccess());
        assertEquals(1, res.getExitStatus());
    }

    @Test(expected = NullPointerException.class)
    public void nullPassword() {
        p.authenticate(uId, null);
    }

    protected abstract void setEmptyLocalpartConfig();

    @Test
    public void emptyLocalpartConfig() {
        setEmptyLocalpartConfig();

        ExecAuthResult res = p.authenticate(uId, requiredPass);
        assertEquals(false, res.isSuccess());
        assertEquals(10, res.getExitStatus());

        setValidConfig();
    }

    public abstract void setWrongLocalpartConfig();

    @Test
    public void wrongLocalpartConfig() {
        setWrongLocalpartConfig();

        ExecAuthResult res = p.authenticate(uId, requiredPass);
        assertEquals(false, res.isSuccess());
        assertEquals(20, res.getExitStatus());

        setValidConfig();
    }

    protected abstract void setEmptyDomainConfig();

    @Test
    public void emptyDomainConfig() {
        setEmptyDomainConfig();

        ExecAuthResult res = p.authenticate(uId, requiredPass);
        assertEquals(false, res.isSuccess());
        assertEquals(11, res.getExitStatus());

        setValidConfig();
    }

    public abstract void setWrongDomainConfig();

    @Test
    public void wrongDomainConfig() {
        setWrongDomainConfig();

        ExecAuthResult res = p.authenticate(uId, requiredPass);
        assertEquals(false, res.isSuccess());
        assertEquals(21, res.getExitStatus());

        setValidConfig();
    }

    protected abstract void setEmptyMxidConfig();

    @Test
    public void emptyMxidConfig() {
        setEmptyMxidConfig();

        ExecAuthResult res = p.authenticate(uId, requiredPass);
        assertEquals(false, res.isSuccess());
        assertEquals(12, res.getExitStatus());

        setValidConfig();
    }

    public abstract void setWrongMxidConfig();

    @Test
    public void wrongMxidConfig() {
        setWrongMxidConfig();

        ExecAuthResult res = p.authenticate(uId, requiredPass);
        assertEquals(false, res.isSuccess());
        assertEquals(22, res.getExitStatus());

        setValidConfig();
    }

}
