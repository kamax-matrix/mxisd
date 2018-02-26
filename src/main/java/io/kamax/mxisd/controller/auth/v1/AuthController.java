/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kamax.mxisd.auth.AuthManager;
import io.kamax.mxisd.auth.UserAuthResult;
import io.kamax.mxisd.controller.auth.v1.io.CredentialsValidationResponse;
import io.kamax.mxisd.controller.auth.v1.io.LoginRequestJson;
import io.kamax.mxisd.controller.auth.v1.io.LoginResponseJson;
import io.kamax.mxisd.exception.JsonMemberNotFoundException;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
import io.kamax.mxisd.util.GsonParser;
import io.kamax.mxisd.util.GsonUtil;
import io.kamax.mxisd.util.RestClientUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class AuthController {

    private Logger log = LoggerFactory.getLogger(AuthController.class);

    private Gson gson = GsonUtil.build();
    private GsonParser parser = new GsonParser(gson);

    @Autowired
    private AuthManager mgr;

    @Autowired
    private LookupStrategy strategy;

    @RequestMapping(value = "/_matrix-internal/identity/v1/check_credentials", method = RequestMethod.POST)
    public String checkCredentials(HttpServletRequest req) {
        try {
            JsonObject authData = parser.parse(req.getInputStream(), "user");
            if (!authData.has("id") || !authData.has("password")) {
                throw new JsonMemberNotFoundException("Missing id or password keys");
            }

            String id = authData.get("id").getAsString();
            log.info("Requested to check credentials for {}", id);
            String password = authData.get("password").getAsString();

            UserAuthResult result = mgr.authenticate(id, password);
            CredentialsValidationResponse response = new CredentialsValidationResponse(result.isSuccess());

            if (result.isSuccess()) {
                response.setDisplayName(result.getDisplayName());
                response.getProfile().setThreePids(result.getThreePids());
            }
            JsonElement authObj = gson.toJsonTree(response);

            JsonObject obj = new JsonObject();
            obj.add("auth", authObj);
            obj.add("authentication", authObj); // TODO remove later, legacy support
            return gson.toJson(obj);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping(value = "/_matrix-internal/identity/v1/login", method = RequestMethod.POST)
    public String login(HttpServletRequest req) {
        try {
            log.info("LOGIN CALLED");

            LoginRequestJson loginRequestJson = parser.parse(req, LoginRequestJson.class);
            log.info("REQUEST: {}", loginRequestJson.toString());

            // find 3PID locally (if requested)
            if (StringUtils.isNotBlank(loginRequestJson.getAddress()) && StringUtils.isNotBlank(loginRequestJson.getMedium())) {
                log.info("finding 3PID locally...");
                Optional<SingleLookupReply> lookupDataOpt = strategy.findLocal(loginRequestJson.getMedium(), loginRequestJson.getAddress());
                if (lookupDataOpt.isPresent()) {
                    log.info("found 3PID: " + lookupDataOpt.toString());
                    SingleLookupReply lookupReply = lookupDataOpt.get();
                    loginRequestJson.setUser(lookupReply.getMxid().getId());
                } else {
                    log.warn("3PID not found");
                    // todo
                }
            }

            // todo login user
            LoginResponseJson loginResponseJson;
            // todo get existing hs url
            HttpPost httpPost = RestClientUtils.post("http://localhost:8008/_matrix/client/r0/login", gson.toJson(loginRequestJson));
            CloseableHttpClient client = HttpClients.createDefault();
            try (CloseableHttpResponse httpResponse = client.execute(httpPost)) {
                int status = httpResponse.getStatusLine().getStatusCode();
                log.info("http status = {}", status);
                if (status < 200 || status >= 300) {
                    // todo - throw new InternalServerError ?
                }
                loginResponseJson = parser.parse(httpResponse, LoginResponseJson.class);
            } catch (IOException e) {
                // todo - throw new InternalServerError ?
                throw e;
            }

            log.info("LOGIN RESPONSE: {}", loginResponseJson.toString());
            String resp = gson.toJson(loginResponseJson);
            return resp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
