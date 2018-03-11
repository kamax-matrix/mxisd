/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sàrl
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

package io.kamax.mxisd.controller.auth.v1;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix.ThreePid;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.backend.google.GoogleProviderBackend;
import io.kamax.mxisd.dns.ClientDnsOverwrite;
import io.kamax.mxisd.profile.ProfileManager;
import io.kamax.mxisd.util.GsonParser;
import io.kamax.mxisd.util.GsonUtil;
import io.kamax.mxisd.util.RestClientUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RegistrationController {

    private final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    private final String registerV1Url = "/_matrix/client/r0/register";

    private GoogleProviderBackend google;
    private ProfileManager pMgr;
    private ClientDnsOverwrite dns;
    private CloseableHttpClient client;
    private Gson gson;
    private GsonParser parser;

    @Autowired
    public RegistrationController(GoogleProviderBackend google, ProfileManager pMgr, ClientDnsOverwrite dns, CloseableHttpClient client) {
        this.google = google;
        this.pMgr = pMgr;
        this.dns = dns;
        this.client = client;
        this.gson = GsonUtil.build();
        this.parser = new GsonParser(gson);
    }

    private String resolveProxyUrl(HttpServletRequest req) {
        URI target = URI.create(req.getRequestURL().toString());
        URIBuilder builder = dns.transform(target);
        String urlToLogin = builder.toString();
        log.info("Proxy resolution: {} to {}", target.toString(), urlToLogin);
        return urlToLogin;
    }

    @RequestMapping(path = registerV1Url, method = RequestMethod.GET)
    public String getLogin(HttpServletRequest req, HttpServletResponse res) {
        try (CloseableHttpResponse hsResponse = client.execute(new HttpGet(resolveProxyUrl(req)))) {
            res.setStatus(hsResponse.getStatusLine().getStatusCode());
            return EntityUtils.toString(hsResponse.getEntity());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping(path = registerV1Url, method = RequestMethod.POST)
    public String register(HttpServletRequest req, HttpServletResponse res) {
        List<ThreePid> ids = new ArrayList<>();
        try {
            JsonObject reqJsonObject = parser.parse(req.getInputStream());
            GsonUtil.findObj(reqJsonObject, "auth").ifPresent(auth -> {
                GsonUtil.findPrimitive(auth, "type").ifPresent(type -> {
                    if (StringUtils.equals("io.kamax.google.auth", type.getAsString())) {
                        log.info("Got registration attempt with Google account");
                        if (!auth.has("googleId")) {
                            throw new IllegalArgumentException("Google ID is missing");
                        }

                        String gId = auth.get("googleId").getAsString();
                        try {
                            GoogleIdToken token = google.extractToken(reqJsonObject.get("password").getAsString()).orElseThrow(() -> new IllegalArgumentException("Google ID Token is missing or invalid"));
                            if (!StringUtils.equals(gId, token.getPayload().getSubject())) {
                                throw new IllegalArgumentException("Google ID does not match token");
                            }
                            log.info("Google ID: {}", gId);

                            ids.addAll(google.extractThreepids(token));

                            auth.addProperty("type", "m.login.dummy");
                            auth.remove("googleId");
                            reqJsonObject.addProperty("username", "g-" + gId);
                            reqJsonObject.addProperty("password", "");
                        } catch (IOException | GeneralSecurityException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            });

            log.info("Sending body: {}", gson.toJson(reqJsonObject));
            HttpPost httpPost = RestClientUtils.post(resolveProxyUrl(req), gson, reqJsonObject);
            try (CloseableHttpResponse httpResponse = client.execute(httpPost)) {
                int sc = httpResponse.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(httpResponse.getEntity());
                JsonObject json = parser.parse(body);
                if (sc == 200 && json.has("user_id")) {
                    // Required here as synapse doesn't call pass provider on register
                    log.info("User was registered, adding 3PIDs");
                    _MatrixID mxid = new MatrixID(json.get("user_id").getAsString());
                    for (ThreePid tpid : ids) {
                        pMgr.addThreepid(mxid, tpid);
                    }
                }
                res.setStatus(sc);
                return body;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
