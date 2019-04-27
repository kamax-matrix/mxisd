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

package io.kamax.mxisd.test.matrix;

import io.kamax.mxisd.Mxisd;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.dns.FederationDnsOverwrite;
import io.kamax.mxisd.matrix.HomeserverFederationResolver;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertEquals;

public class HomeserverFederationResolverTest {

    private static HomeserverFederationResolver resolver;

    @BeforeClass
    public static void beforeClass() {
        CloseableHttpClient client = HttpClients.custom()
                .setUserAgent(Mxisd.Agent)
                .setMaxConnPerRoute(Integer.MAX_VALUE)
                .setMaxConnTotal(Integer.MAX_VALUE)
                .build();

        FederationDnsOverwrite fedDns = new FederationDnsOverwrite(new MxisdConfig().getDns().getOverwrite());
        resolver = new HomeserverFederationResolver(fedDns, client);
    }

    @Test
    public void hostnameWithoutPort() {
        URL url = resolver.resolve("example.org");
        assertEquals("https://example.org:8448", url.toString());
    }

    @Test
    public void hostnameWithPort() {
        URL url = resolver.resolve("example.org:443");
        assertEquals("https://example.org:443", url.toString());
    }

}
