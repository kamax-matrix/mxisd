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
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.kamax.mxisd.auth.AuthManager;
import io.kamax.mxisd.auth.UserAuthResult;
import io.kamax.mxisd.controller.auth.v1.io.CredentialsValidationResponse;
import io.kamax.mxisd.dns.ClientDnsOverwrite;
import io.kamax.mxisd.exception.JsonMemberNotFoundException;
import io.kamax.mxisd.exception.RemoteLoginException;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
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

@RestController
@CrossOrigin
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class AuthController {

    // TODO export into SDK
    private static final String logV1Url = "/_matrix/client/r0/login";

    private Logger log = LoggerFactory.getLogger(AuthController.class);

    private Gson gson = GsonUtil.build();
    private GsonParser parser = new GsonParser(gson);

    @Autowired
    private AuthManager mgr;

    @Autowired
    private LookupStrategy strategy;

    @Autowired
    private ClientDnsOverwrite dns;

    @Autowired
    private CloseableHttpClient client;

    private String resolveProxyUrl(HttpServletRequest req) {
        URI target = URI.create(req.getRequestURL().toString());
        URIBuilder builder = dns.transform(target);
        String urlToLogin = builder.toString();
        log.info("Proxy resolution: {} to {}", target.toString(), urlToLogin);
        return urlToLogin;
    }

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

    @RequestMapping(value = logV1Url, method = RequestMethod.GET)
    public String getLogin(HttpServletRequest req, HttpServletResponse res) {
        try (CloseableHttpResponse hsResponse = client.execute(new HttpGet(resolveProxyUrl(req)))) {
            res.setStatus(hsResponse.getStatusLine().getStatusCode());
            return EntityUtils.toString(hsResponse.getEntity());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping(value = logV1Url, method = RequestMethod.POST)
    public String login(HttpServletRequest req) {
        try {
            JsonObject reqJsonObject = parser.parse(req.getInputStream());

            // find 3PID in main object
            GsonUtil.findPrimitive(reqJsonObject, "medium").ifPresent(medium -> {
                GsonUtil.findPrimitive(reqJsonObject, "address").ifPresent(address -> {
                    log.info("Login request with medium '{}' and address '{}'", medium.getAsString(), address.getAsString());
                    strategy.findLocal(medium.getAsString(), address.getAsString()).ifPresent(lookupDataOpt -> {
                        reqJsonObject.addProperty("user", lookupDataOpt.getMxid().getLocalPart());
                        reqJsonObject.remove("medium");
                        reqJsonObject.remove("address");
                    });
                });
            });

            // find 3PID in 'identifier' object
            GsonUtil.findObj(reqJsonObject, "identifier").ifPresent(identifier -> {
                GsonUtil.findPrimitive(identifier, "type").ifPresent(type -> {

                    if (StringUtils.equals(type.getAsString(), "m.id.thirdparty")) {
                        GsonUtil.findPrimitive(identifier, "medium").ifPresent(medium -> {
                            GsonUtil.findPrimitive(identifier, "address").ifPresent(address -> {
                                log.info("Login request with medium '{}' and address '{}'", medium.getAsString(), address.getAsString());
                                strategy.findLocal(medium.getAsString(), address.getAsString()).ifPresent(lookupDataOpt -> {
                                    identifier.addProperty("type", "m.id.user");
                                    identifier.addProperty("user", lookupDataOpt.getMxid().getLocalPart());
                                    identifier.remove("medium");
                                    identifier.remove("address");
                                });
                            });
                        });
                    }

                    if (StringUtils.equals(type.getAsString(), "m.id.phone")) {
                        GsonUtil.findPrimitive(identifier, "number").ifPresent(number -> {
                            GsonUtil.findPrimitive(identifier, "country").ifPresent(country -> {
                                log.info("Login request with phone '{}'-'{}'", country.getAsString(), number.getAsString());
                                try {
                                    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                                    Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(number.getAsString(), country.getAsString());
                                    String canon_phoneNumber = phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164).replace("+", "");
                                    String medium = "msisdn";
                                    strategy.findLocal(medium, canon_phoneNumber).ifPresent(lookupDataOpt -> {
                                        identifier.addProperty("type", "m.id.user");
                                        identifier.addProperty("user", lookupDataOpt.getMxid().getLocalPart());
                                        identifier.remove("country");
                                        identifier.remove("number");
                                    });
                                } catch (NumberParseException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        });
                    }
                });
            });

            // invoke 'login' on homeserver
            HttpPost httpPost = RestClientUtils.post(resolveProxyUrl(req), gson, reqJsonObject);
            try (CloseableHttpResponse httpResponse = client.execute(httpPost)) {
                // check http status
                int status = httpResponse.getStatusLine().getStatusCode();
                log.info("http status = {}", status);
                if (status != 200) {
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

                /// return response
                JsonObject respJsonObject = parser.parseOptional(httpResponse).get();
                return gson.toJson(respJsonObject);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
