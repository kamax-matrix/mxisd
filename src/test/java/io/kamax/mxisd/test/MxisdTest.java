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

package io.kamax.mxisd.test;

import io.kamax.mxisd.Mxisd;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.config.memory.MemoryIdentityConfig;
import io.kamax.mxisd.config.memory.MemoryThreePid;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class MxisdTest {

    private Mxisd m;

    @Before
    public void before() {
        MxisdConfig cfg = MxisdConfig.forDomain("localhost").inMemory();

        MemoryThreePid mem3pid = new MemoryThreePid();
        mem3pid.setMedium("email");
        mem3pid.setAddress("john@localhost");
        MemoryIdentityConfig memCfg = new MemoryIdentityConfig();
        memCfg.setUsername("john");
        memCfg.getThreepids().add(mem3pid);
        cfg.getMemory().setEnabled(true);
        cfg.getMemory().getIdentities().add(memCfg);

        m = new Mxisd(cfg);
        m.start();
    }

    @After
    public void after() {
        m.stop();
    }

    @Test
    public void singleLookup() {
        SingleLookupRequest req = new SingleLookupRequest();
        req.setRecursive(false);
        req.setType("email");
        req.setThreePid("john@localhost");
        Optional<SingleLookupReply> reply = m.getIdentity().find(req);
        assertTrue(reply.isPresent());
        assertEquals("@john:localhost", reply.get().getMxid().getId());
    }

}
