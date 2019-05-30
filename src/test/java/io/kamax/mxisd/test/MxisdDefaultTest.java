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

import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.Mxisd;
import io.kamax.mxisd.config.MxisdConfig;
import org.junit.Test;

import static junit.framework.TestCase.*;

public class MxisdDefaultTest {

    private final String domain = "localhost";

    @Test
    public void defaultConfig() {
        MxisdConfig cfg = MxisdConfig.forDomain(domain).inMemory();
        Mxisd m = new Mxisd(cfg);
        m.start();

        assertNotNull(m.getConfig());
        assertEquals(domain, m.getConfig().getMatrix().getDomain());

        assertTrue(m.getNotif().isMediumSupported(ThreePidMedium.Email.getId()));
        assertTrue(m.getNotif().isMediumSupported(ThreePidMedium.PhoneNumber.getId()));
    }

}
