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

import io.kamax.mxisd.backend.exec.ExecStore;
import io.kamax.mxisd.test.backend.exec.ExecAuthStoreTest;

public class ExecAuthInputMultilinesTest extends ExecAuthStoreTest {

    @Override
    protected void setValidCommand() {
        cfg.getAuth().setCommand("src/test/resources/store/exec/input/multilinesTest.sh");
    }

    @Override
    protected void setValidInput() {
        cfg.getAuth().getInput().setType(ExecStore.PlainType);
        cfg.getAuth().getInput().setTemplate(null);
    }

    @Override
    protected void setEmptyLocalpartConfig() {
        cfg.getAuth().getInput().setTemplate("" + System.lineSeparator()
                + DomainToken + System.lineSeparator()
                + MxidToken + System.lineSeparator()
                + PassToken + System.lineSeparator()
        );
    }

    @Override
    public void setWrongLocalpartConfig() {
        cfg.getAuth().getInput().setTemplate(LocalpartInvalid + System.lineSeparator()
                + DomainToken + System.lineSeparator()
                + MxidToken + System.lineSeparator()
                + PassToken + System.lineSeparator()
        );
    }

    @Override
    protected void setEmptyDomainConfig() {
        cfg.getAuth().getInput().setTemplate(LocalpartToken + System.lineSeparator()
                + "" + System.lineSeparator()
                + MxidToken + System.lineSeparator()
                + PassToken + System.lineSeparator()
        );
    }

    @Override
    public void setWrongDomainConfig() {
        cfg.getAuth().getInput().setTemplate(LocalpartToken + System.lineSeparator()
                + DomainInvalid + System.lineSeparator()
                + MxidToken + System.lineSeparator()
                + PassToken + System.lineSeparator()
        );
    }

    @Override
    protected void setEmptyMxidConfig() {
        cfg.getAuth().getInput().setTemplate(LocalpartToken + System.lineSeparator()
                + DomainToken + System.lineSeparator()
                + "" + System.lineSeparator()
                + PassToken + System.lineSeparator()
        );
    }

    @Override
    public void setWrongMxidConfig() {
        cfg.getAuth().getInput().setTemplate(LocalpartToken + System.lineSeparator()
                + DomainToken + System.lineSeparator()
                + MxidInvalid + System.lineSeparator()
                + PassToken + System.lineSeparator()
        );
    }

}
