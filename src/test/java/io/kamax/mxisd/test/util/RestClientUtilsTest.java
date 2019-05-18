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

package io.kamax.mxisd.test.util;

import io.kamax.mxisd.util.RestClientUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RestClientUtilsTest {

    @Test
    public void urlEncode() {
        String encoded = RestClientUtils.urlEncode("john+doe@example.org");
        assertEquals("john%2Bdoe%40example.org", encoded);
    }

}
