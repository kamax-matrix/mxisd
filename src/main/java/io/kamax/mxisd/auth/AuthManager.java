/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Kamax Sarl
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

package io.kamax.mxisd.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix.ThreePid;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix._ThreePid;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.auth.provider.BackendAuthResult;
import io.kamax.mxisd.config.AuthenticationConfig;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.dns.ClientDnsOverwrite;
import io.kamax.mxisd.exception.RemoteLoginException;
import io.kamax.mxisd.invitation.InvitationManager;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
import io.kamax.mxisd.util.RestClientUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AuthManager {

    private static final Logger log = LoggerFactory.getLogger(AuthManager.class);

    private static final String TypeKey = "type";
    private static final String UserKey = "user";
    private static final String IdentifierKey = "identifier";
    private static final String ThreepidMediumKey = "medium";
    private static final String ThreepidAddressKey = "address";
    private static final String UserIdTypeValue = "m.id.user";
    private static final String ThreepidTypeValue = "m.id.thirdparty";

    private final Gson gson = GsonUtil.get(); // FIXME replace

    private List<AuthenticatorProvider> providers;
    private MatrixConfig mxCfg;
    private AuthenticationConfig cfg;
    private InvitationManager invMgr;
    private ClientDnsOverwrite dns;
    private LookupStrategy strategy;
    private CloseableHttpClient client;

    public AuthManager(
            MxisdConfig cfg,
            List<? extends AuthenticatorProvider> providers,
            LookupStrategy strategy,
            InvitationManager invMgr,
            ClientDnsOverwrite dns,
            CloseableHttpClient client
    ) {
        this.cfg = cfg.getAuth();
        this.mxCfg = cfg.getMatrix();
        this.providers = new ArrayList<>(providers);
        this.strategy = strategy;
        this.invMgr = invMgr;
        this.dns = dns;
        this.client = client;
    }

    public String resolveProxyUrl(URI target) {
        URIBuilder builder = dns.transform(target);
        String urlToLogin = builder.toString();
        log.info("Proxy resolution: {} to {}", target.toString(), urlToLogin);
        return urlToLogin;
    }

    public UserAuthResult authenticate(String id, String password) {
        _MatrixID mxid = MatrixID.asAcceptable(id);
        for (AuthenticatorProvider provider : providers) {
            if (!provider.isEnabled()) {
                continue;
            }

            log.info("Attempting authentication with store {}", provider.getClass().getSimpleName());

            BackendAuthResult result = provider.authenticate(mxid, password);
            if (result.isSuccess()) {
                String mxId;
                if (UserIdType.Localpart.is(result.getId().getType())) {
                    mxId = MatrixID.from(result.getId().getValue(), mxCfg.getDomain()).acceptable().getId();
                } else if (UserIdType.MatrixID.is(result.getId().getType())) {
                    mxId = MatrixID.asAcceptable(result.getId().getValue()).getId();
                } else {
                    log.warn("Unsupported User ID type {} for backend {}", result.getId().getType(), provider.getClass().getSimpleName());
                    continue;
                }

                UserAuthResult authResult = new UserAuthResult().success(result.getProfile().getDisplayName());
                for (_ThreePid pid : result.getProfile().getThreePids()) {
                    authResult.withThreePid(pid.getMedium(), pid.getAddress());
                }
                log.info("{} was authenticated by {}, publishing 3PID mappings, if any", id, provider.getClass().getSimpleName());
                for (ThreePid pid : authResult.getThreePids()) {
                    log.info("Processing {} for {}", pid, id);
                    invMgr.publishMappingIfInvited(new ThreePidMapping(pid, mxId));
                }

                try {
                    MatrixID.asValid(mxId);
                } catch (IllegalArgumentException e) {
                    log.warn("The returned User ID {} is not a valid Matrix ID. Login might fail at the Homeserver level", mxId);
                }

                invMgr.lookupMappingsForInvites();

                return authResult;
            }
        }

        return new UserAuthResult().failure();
    }

    public String proxyLogin(URI target, String body) {
        JsonObject reqJsonObject = io.kamax.matrix.json.GsonUtil.parseObj(body);

        GsonUtil.findObj(reqJsonObject, IdentifierKey).ifPresent(obj -> {
            GsonUtil.findString(obj, TypeKey).ifPresent(type -> {
                if (StringUtils.equals(type, UserIdTypeValue)) {
                    log.info("Login request is User ID type");

                    if (cfg.getRewrite().getUser().getRules().isEmpty()) {
                        log.info("No User ID rewrite rules to apply");
                    } else {
                        log.info("User ID rewrite rules: checking for a match");

                        String userId = GsonUtil.getStringOrThrow(obj, UserKey);
                        for (AuthenticationConfig.Rule m : cfg.getRewrite().getUser().getRules()) {
                            if (m.getPattern().matcher(userId).matches()) {
                                log.info("Found matching pattern, resolving to 3PID with medium {}", m.getMedium());

                                // Remove deprecated login info on the top object if exists to avoid duplication
                                reqJsonObject.remove(UserKey);
                                obj.addProperty(TypeKey, ThreepidTypeValue);
                                obj.addProperty(ThreepidMediumKey, m.getMedium());
                                obj.addProperty(ThreepidAddressKey, userId);

                                log.info("Rewrite to 3PID done");
                            }
                        }

                        log.info("User ID rewrite rules: done checking rules");
                    }
                }
            });
        });

        GsonUtil.findObj(reqJsonObject, IdentifierKey).ifPresent(obj -> {
            GsonUtil.findString(obj, TypeKey).ifPresent(type -> {
                if (StringUtils.equals(type, ThreepidTypeValue)) {
                    // Remove deprecated login info if exists to avoid duplication
                    reqJsonObject.remove(ThreepidMediumKey);
                    reqJsonObject.remove(ThreepidAddressKey);

                    GsonUtil.findPrimitive(obj, ThreepidMediumKey).ifPresent(medium -> {
                        GsonUtil.findPrimitive(obj, ThreepidAddressKey).ifPresent(address -> {
                            log.info("Login request with medium '{}' and address '{}'", medium.getAsString(), address.getAsString());
                            strategy.findLocal(medium.getAsString(), address.getAsString()).ifPresent(lookupDataOpt -> {
                                obj.remove(ThreepidMediumKey);
                                obj.remove(ThreepidAddressKey);
                                obj.addProperty(TypeKey, UserIdTypeValue);
                                obj.addProperty(UserKey, lookupDataOpt.getMxid().getLocalPart());
                            });
                        });
                    });
                }

                if (StringUtils.equals(type, "m.id.phone")) {
                    // Remove deprecated login info if exists to avoid duplication
                    reqJsonObject.remove(ThreepidMediumKey);
                    reqJsonObject.remove(ThreepidAddressKey);

                    GsonUtil.findPrimitive(obj, "number").ifPresent(number -> {
                        GsonUtil.findPrimitive(obj, "country").ifPresent(country -> {
                            log.info("Login request with phone '{}'-'{}'", country.getAsString(), number.getAsString());
                            try {
                                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                                Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(number.getAsString(), country.getAsString());
                                String msisdn = phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164).replace("+", "");
                                String medium = "msisdn";
                                strategy.findLocal(medium, msisdn).ifPresent(lookupDataOpt -> {
                                    obj.remove("country");
                                    obj.remove("number");
                                    obj.addProperty(TypeKey, UserIdTypeValue);
                                    obj.addProperty(UserKey, lookupDataOpt.getMxid().getLocalPart());
                                });
                            } catch (NumberParseException e) {
                                log.error("Not a valid phone number");
                                throw new RuntimeException(e);
                            }
                        });
                    });
                }
            });
        });

        // invoke 'login' on homeserver
        HttpPost httpPost = RestClientUtils.post(resolveProxyUrl(target), gson, reqJsonObject);
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

            // return response
            HttpEntity entity = httpResponse.getEntity();
            if (Objects.isNull(entity)) {
                log.warn("Expected HS to return data but got nothing");
                return "";
            } else {
                return IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
