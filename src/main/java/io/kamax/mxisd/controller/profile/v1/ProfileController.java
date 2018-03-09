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

package io.kamax.mxisd.controller.profile.v1;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.kamax.matrix.MatrixID;
import io.kamax.mxisd.ThreePid;
import io.kamax.mxisd.dns.ClientDnsOverwrite;
import io.kamax.mxisd.profile.ProfileManager;
import io.kamax.mxisd.util.GsonUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;

@RestController
@CrossOrigin
@RequestMapping(path = "/_matrix/client/r0/profile", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ProfileController {

    private final Logger log = LoggerFactory.getLogger(ProfileController.class);
    private final ProfileManager mgr;
    private final CloseableHttpClient client;
    private final ClientDnsOverwrite dns;
    private final JsonParser parser;
    private final Gson gson;

    @Autowired
    public ProfileController(ProfileManager mgr, CloseableHttpClient client, ClientDnsOverwrite dns) {
        this.mgr = mgr;
        this.client = client;
        this.dns = dns;
        this.parser = new JsonParser();
        this.gson = GsonUtil.build();
    }

    // FIXME do properly in the SDK (headers, check access token, etc.)
    private String resolveProxyUrl(HttpServletRequest req) {
        URI target = URI.create(req.getRequestURL().toString() + (Objects.isNull(req.getQueryString()) ? "" : "?" + req.getQueryString()));
        URIBuilder builder = dns.transform(target);
        String urlToLogin = builder.toString();
        log.info("Proxy resolution: {} to {}", target.toString(), urlToLogin);
        return urlToLogin;
    }

    @RequestMapping("/{userId:.+}")
    public String getProfile(HttpServletRequest req, HttpServletResponse res, @PathVariable String userId) {
        try (CloseableHttpResponse hsResponse = client.execute(new HttpGet(resolveProxyUrl(req)))) { //
            res.setStatus(hsResponse.getStatusLine().getStatusCode());
            JsonElement el = parser.parse(EntityUtils.toString(hsResponse.getEntity()));
            List<ThreePid> list = mgr.getThreepids(new MatrixID(userId));
            if (!list.isEmpty() && el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                obj.add("threepids", GsonUtil.build().toJsonTree(list));
            }
            return gson.toJson(el);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
