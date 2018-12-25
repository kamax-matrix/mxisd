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

import java.util.Arrays;

public class ExecAuthArgsTest extends ExecAuthStoreTest {

    @Override
    protected void setValidCommand() {
        cfg.getAuth().setCommand("src/test/resources/store/exec/input/argsTest.sh");
    }

    @Override
    protected void setValidArgs() {
        cfg.getAuth().setArgs(Arrays.asList(LocalpartToken, DomainToken, MxidToken, PassToken));
    }

    @Override
    protected void setEmptyLocalpartConfig() {
        cfg.getAuth().setArgs(Arrays.asList("", DomainToken, MxidToken, PassToken));
    }

    @Override
    public void setWrongLocalpartConfig() {
        cfg.getAuth().setArgs(Arrays.asList(LocalpartInvalid, DomainToken, MxidToken, PassToken));
    }

    @Override
    protected void setEmptyDomainConfig() {
        cfg.getAuth().setArgs(Arrays.asList(LocalpartToken, "", MxidToken, PassToken));
    }

    @Override
    public void setWrongDomainConfig() {
        cfg.getAuth().setArgs(Arrays.asList(LocalpartToken, DomainInvalid, MxidToken, PassToken));
    }

    @Override
    protected void setEmptyMxidConfig() {
        cfg.getAuth().setArgs(Arrays.asList(LocalpartToken, DomainToken, "", PassToken));
    }

    @Override
    public void setWrongMxidConfig() {
        cfg.getAuth().setArgs(Arrays.asList(LocalpartToken, DomainToken, MxidInvalid, PassToken));
    }

}
