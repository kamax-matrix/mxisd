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

package io.kamax.mxisd.test.auth;

import io.kamax.mxisd.Mxisd;
import io.kamax.mxisd.auth.AuthManager;
import io.kamax.mxisd.auth.UserAuthResult;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.config.memory.MemoryIdentityConfig;
import io.kamax.mxisd.config.memory.MemoryThreePid;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AuthManagerTest {

    private static AuthManager mgr;

    // FIXME we should be able to easily build the class ourselves
    // FIXME use constants
    @BeforeClass
    public static void beforeClass() {
        MxisdConfig cfg = new MxisdConfig();
        cfg.getMatrix().setDomain("localhost");
        cfg.getKey().setPath(":memory:");
        cfg.getStorage().getProvider().getSqlite().setDatabase(":memory:");

        MemoryThreePid mem3pid = new MemoryThreePid();
        mem3pid.setMedium("email");
        mem3pid.setAddress("john@localhost");
        MemoryIdentityConfig validCfg = new MemoryIdentityConfig();
        validCfg.setUsername("john");
        validCfg.setPassword("doe");
        validCfg.getThreepids().add(mem3pid);
        MemoryIdentityConfig illegalUser = new MemoryIdentityConfig();
        illegalUser.setUsername("JANE");
        illegalUser.setPassword("doe");
        cfg.getMemory().setEnabled(true);
        cfg.getMemory().getIdentities().add(validCfg);
        cfg.getMemory().getIdentities().add(illegalUser);

        Mxisd m = new Mxisd(cfg);
        m.start();
        mgr = m.getAuth();
    }

    @Test
    public void basic() {
        UserAuthResult result = mgr.authenticate("@john:localhost", "doe");
        assertTrue(result.isSuccess());

        // For backward-compatibility as per instructed by the spec, we do not fail on an illegal username
        // This makes sure we don't break it
        result = mgr.authenticate("@JANE:localhost", "doe");
        assertTrue(result.isSuccess());
    }

}
