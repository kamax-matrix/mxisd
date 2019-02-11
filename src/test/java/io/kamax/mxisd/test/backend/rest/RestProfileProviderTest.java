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

package io.kamax.mxisd.test.backend.rest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix._ThreePid;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.backend.rest.RestProfileProvider;
import io.kamax.mxisd.config.rest.RestBackendConfig;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.profile.JsonProfileRequest;
import io.kamax.mxisd.profile.JsonProfileResult;
import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.kamax.mxisd.config.rest.RestBackendConfig.ProfileEndpoints;
import static org.junit.Assert.*;

public class RestProfileProviderTest {

    private static final int MockHttpPort = 65000;
    private static final String MockHttpHost = "localhost";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(MockHttpPort);

    private final String displayNameEndpoint = "/displayName";

    private final _MatrixID userId = MatrixID.from("john", "matrix." + MockHttpHost).valid();

    private RestProfileProvider p;

    private RestBackendConfig getCfg(RestBackendConfig.Endpoints endpoints) {
        RestBackendConfig cfg = new RestBackendConfig();
        cfg.setEnabled(true);
        cfg.setHost("http://" + MockHttpHost + ":" + MockHttpPort);
        cfg.setEndpoints(endpoints);
        cfg.build();

        return cfg;
    }

    private RestProfileProvider get(RestBackendConfig cfg) {
        return new RestProfileProvider(cfg);
    }

    @Before
    public void before() {
        ProfileEndpoints pEndpoints = new ProfileEndpoints();
        pEndpoints.setDisplayName(displayNameEndpoint);
        RestBackendConfig.Endpoints endpoints = new RestBackendConfig.Endpoints();
        endpoints.setProfile(pEndpoints);

        p = get(getCfg(endpoints));
    }

    @Test
    public void forNameFound() {
        String value = "This is my display name";

        JsonProfileResult r = new JsonProfileResult();
        r.setDisplayName(value);
        stubFor(post(urlEqualTo(displayNameEndpoint))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(GsonUtil.get().toJson(GsonUtil.makeObj("profile", r)))
                )
        );

        Optional<String> v = p.getDisplayName(userId);

        verify(postRequestedFor(urlMatching(displayNameEndpoint))
                .withHeader("Content-Type", containing(ContentType.APPLICATION_JSON.getMimeType()))
                .withRequestBody(equalTo(GsonUtil.get().toJson(new JsonProfileRequest(userId))))
        );

        assertTrue(v.isPresent());
        assertEquals(value, v.get());
    }

    @Test
    public void forNameNotFound() {
        stubFor(post(urlEqualTo(displayNameEndpoint))
                .willReturn(aResponse()
                        .withStatus(404)
                )
        );

        Optional<String> v = p.getDisplayName(userId);

        verify(postRequestedFor(urlMatching(displayNameEndpoint))
                .withHeader("Content-Type", containing(ContentType.APPLICATION_JSON.getMimeType()))
                .withRequestBody(equalTo(GsonUtil.get().toJson(new JsonProfileRequest(userId))))
        );

        assertFalse(v.isPresent());
    }

    @Test
    public void forNameEmptyBody() {
        stubFor(post(urlEqualTo(displayNameEndpoint))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                )
        );

        Optional<String> v = p.getDisplayName(userId);

        verify(postRequestedFor(urlMatching(displayNameEndpoint))
                .withHeader("Content-Type", containing(ContentType.APPLICATION_JSON.getMimeType()))
                .withRequestBody(equalTo(GsonUtil.get().toJson(new JsonProfileRequest(userId))))
        );

        assertFalse(v.isPresent());
    }

    @Test(expected = InternalServerError.class)
    public void forNameInvalidBody() {
        stubFor(post(urlEqualTo(displayNameEndpoint))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("This is not a valid JSON object")
                )
        );

        try {
            p.getDisplayName(userId);
        } finally {
            verify(postRequestedFor(urlMatching(displayNameEndpoint))
                    .withHeader("Content-Type", containing(ContentType.APPLICATION_JSON.getMimeType()))
                    .withRequestBody(equalTo(GsonUtil.get().toJson(new JsonProfileRequest(userId))))
            );
        }
    }

    @Test
    public void forEmptyEndpoints() {
        ProfileEndpoints pEndpoints = new ProfileEndpoints();
        pEndpoints.setDisplayName("");
        pEndpoints.setThreepids("");
        pEndpoints.setRoles("");

        RestBackendConfig.Endpoints endpoints = new RestBackendConfig.Endpoints();
        endpoints.setProfile(pEndpoints);

        RestProfileProvider p = get(getCfg(endpoints));

        Optional<String> dn = p.getDisplayName(userId);
        assertFalse(dn.isPresent());

        List<String> roles = p.getRoles(userId);
        assertTrue(roles.isEmpty());

        List<_ThreePid> tpids = p.getThreepids(userId);
        assertTrue(tpids.isEmpty());
    }

}
