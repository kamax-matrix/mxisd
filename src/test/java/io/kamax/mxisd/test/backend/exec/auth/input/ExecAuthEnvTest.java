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

package io.kamax.mxisd.test.backend.exec.auth.input;

import io.kamax.mxisd.test.backend.exec.ExecAuthStoreTest;

import java.util.HashMap;

public class ExecAuthEnvTest extends ExecAuthStoreTest {

    private final String LocalpartEnv = "LOCALPART";
    private final String DomainEnv = "DOMAIN";
    private final String MxidEnv = "MXID";

    @Override
    protected void setValidCommand() {
        cfg.getAuth().setCommand("src/test/resources/store/exec/input/envTest.sh");
    }

    @Override
    protected void setValidEnv() {
        cfg.getAuth().setEnv(new HashMap<>());
        cfg.getAuth().addEnv(LocalpartEnv, LocalpartToken);
        cfg.getAuth().addEnv(DomainEnv, DomainToken);
        cfg.getAuth().addEnv(MxidEnv, MxidToken);
        cfg.getAuth().addEnv("PASS", PassToken);
    }

    @Override
    protected void setEmptyLocalpartConfig() {
        cfg.getAuth().addEnv(LocalpartEnv, "");
    }

    @Override
    public void setWrongLocalpartConfig() {
        cfg.getAuth().addEnv(LocalpartEnv, LocalpartInvalid);
    }

    @Override
    protected void setEmptyDomainConfig() {
        cfg.getAuth().addEnv(DomainEnv, "");
    }

    @Override
    public void setWrongDomainConfig() {
        cfg.getAuth().addEnv(DomainEnv, DomainInvalid);
    }

    @Override
    protected void setEmptyMxidConfig() {
        cfg.getAuth().addEnv(MxidEnv, "");
    }

    @Override
    public void setWrongMxidConfig() {
        cfg.getAuth().addEnv(MxidEnv, MxidInvalid);
    }

}
