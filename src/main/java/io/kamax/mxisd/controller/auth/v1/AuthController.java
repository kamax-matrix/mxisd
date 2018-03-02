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

import com.google.gson.*;
import io.kamax.mxisd.auth.AuthManager;
import io.kamax.mxisd.auth.UserAuthResult;
import io.kamax.mxisd.controller.auth.v1.io.CredentialsValidationResponse;
import io.kamax.mxisd.controller.auth.v1.io.LoginRequestJson;
import io.kamax.mxisd.controller.auth.v1.io.LoginResponseJson;
import io.kamax.mxisd.dns.ClientDnsOverwrite;
import io.kamax.mxisd.exception.JsonMemberNotFoundException;
import io.kamax.mxisd.exception.RemoteLoginException;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
import io.kamax.mxisd.util.GsonParser;
import io.kamax.mxisd.util.GsonUtil;
import io.kamax.mxisd.util.RestClientUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
import java.io.IOException;
import java.net.URI;
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

    @Autowired
    private ClientDnsOverwrite dns;

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

    @RequestMapping(value = "/_matrix/client/r0/login", method = RequestMethod.GET)
    public String getLogin(HttpServletRequest req) {
        /**
         * This GET method on 'login' is to prevent Riot to display error message at login page.
         *
         * todo: shall be implemented appropriately
         *
         * Current implementation returns:
         *
         *  {
         *      "flows": [
         *          {
         *             "type": "m.login.password"
         *          }
         *      ]
         *  }
         *
         */
        JsonObject flowsJson = new JsonObject();
        JsonArray flowsArray = new JsonArray();
        JsonObject typeJson = new JsonObject();
        typeJson.addProperty("type", "m.login.password");
        flowsArray.add(typeJson);
        flowsJson.add("flows", flowsArray);
        String response = gson.toJson(flowsJson);
        return response;
    }

    @RequestMapping(value = "/_matrix/client/r0/login", method = RequestMethod.POST)
    public String login(HttpServletRequest req) {
        try {
            LoginRequestJson loginRequestJson = parser.parse(req, LoginRequestJson.class);

            // try to find 3PID locally (considering possible 'medium' and 'address' in request)
            log.info("Searching for 3PID locally...");
            if (loginRequestJson.getIdentifier() != null) {
                Optional<SingleLookupReply> lookupDataOpt = strategy.findLocal(
                        loginRequestJson.getIdentifier().getMedium(),
                        loginRequestJson.getIdentifier().getAddress());
                if (lookupDataOpt.isPresent()) {
                    SingleLookupReply lookupReply = lookupDataOpt.get();
                    loginRequestJson.getIdentifier().setUser(lookupReply.getMxid().getLocalPart());
                    log.info("Found 3PID mapping: {medium: '" + loginRequestJson.getIdentifier().getMedium()
                            + "', address: '" + loginRequestJson.getIdentifier().getAddress()
                            + "', user: '" + loginRequestJson.getIdentifier().getUser() + "'}");
                    // must remove 'medium' and 'address' to invoke login using 'user' property
                    loginRequestJson.getIdentifier().setMedium(null);
                    loginRequestJson.getIdentifier().setAddress(null);
                } else {
                    log.warn("3PID not found");
                }
            }

            // obtain url to homeserver 'login' operation using "dns overwrite"
            URI target = URI.create(req.getRequestURL().toString());
            log.info("Url from request: {}", target.toString());
            URIBuilder builder = dns.transform(target);
            String urlToLogin = builder.toString();
            log.info("Querying HS at: {}", urlToLogin);

            // invoke 'login' on homeserver
            LoginResponseJson loginResponseJson;
            HttpPost httpPost = RestClientUtils.post(urlToLogin, gson.toJson(loginRequestJson));
            CloseableHttpClient client = HttpClients.createDefault();
            try (CloseableHttpResponse httpResponse = client.execute(httpPost)) {

                // check http status
                int status = httpResponse.getStatusLine().getStatusCode();
                log.info("http status = {}", status);
                if (status == 200) {
                    loginResponseJson = parser.parse(httpResponse, LoginResponseJson.class);
                } else {

                    // try to get possible json error message from response
                    // otherwise just get returned plain error message
                    String errcode = String.valueOf(httpResponse.getStatusLine().getStatusCode());
                    String error = EntityUtils.toString(httpResponse.getEntity());
                    if (httpResponse.getEntity() != null) {
                        try {
                            JsonObject bodyJson = new JsonParser().parse(error).getAsJsonObject();
                            if (bodyJson.has("errcode")) {
                                errcode = bodyJson.get("errcode").getAsString();
                            }
                            if (bodyJson.has("error")) {
                                error = bodyJson.get("error").getAsString();
                            }
                            throw new RemoteLoginException(status, errcode, error, bodyJson);
                        } catch (JsonSyntaxException e) {
                            log.warn("Response body is not JSON");
                        }
                    }
                    throw new RemoteLoginException(status, errcode, error);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // return 'login' response
            String resp = gson.toJson(loginResponseJson);
            return resp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
