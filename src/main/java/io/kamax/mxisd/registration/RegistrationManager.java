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

package io.kamax.mxisd.registration;

import com.google.gson.JsonObject;
import io.kamax.matrix.ThreePid;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.RegisterConfig;
import io.kamax.mxisd.dns.ClientDnsOverwrite;
import io.kamax.mxisd.exception.NotImplementedException;
import io.kamax.mxisd.exception.RemoteHomeServerException;
import io.kamax.mxisd.invitation.InvitationManager;
import io.kamax.mxisd.util.RestClientUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegistrationManager {

    private static final Logger log = LoggerFactory.getLogger(RegistrationManager.class);

    private final RegisterConfig cfg;
    private final CloseableHttpClient client;
    private final ClientDnsOverwrite dns;
    private final InvitationManager invMgr;

    public RegistrationManager(RegisterConfig cfg, CloseableHttpClient client, ClientDnsOverwrite dns, InvitationManager invMgr) {
        this.cfg = cfg;
        this.client = client;
        this.dns = dns;
        this.invMgr = invMgr;
    }

    private String resolveProxyUrl(URI target) {
        URIBuilder builder = dns.transform(target);
        String urlToLogin = builder.toString();
        log.info("Proxy resolution: {} to {}", target.toString(), urlToLogin);
        return urlToLogin;
    }

    public RegistrationReply execute(URI target, JsonObject request) {
        HttpPost registerProxyRq = RestClientUtils.post(resolveProxyUrl(target), GsonUtil.get(), request);
        try (CloseableHttpResponse response = client.execute(registerProxyRq)) {
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                // The user managed to register. We check if it had a session
                String sessionId = GsonUtil.findObj(request, "auth").flatMap(auth -> GsonUtil.findString(auth, "session")).orElse("");
                if (StringUtils.isEmpty(sessionId)) {
                    // No session ID was provided. This is an edge case we do not support for now as investigation is needed
                    // to ensure how and when this happens.

                    HttpPost newSessReq = RestClientUtils.post(resolveProxyUrl(target), GsonUtil.get(), new JsonObject());
                    try (CloseableHttpResponse newSessRes = client.execute(newSessReq)) {
                        RegistrationReply reply = new RegistrationReply();
                        reply.setStatus(newSessRes.getStatusLine().getStatusCode());
                        reply.setBody(GsonUtil.parseObj(EntityUtils.toString(newSessRes.getEntity())));
                        return reply;
                    }
                }
            }

            throw new NotImplementedException("Registration");
        } catch (IOException e) {
            throw new RemoteHomeServerException(e.getMessage());
        }
    }

    public boolean isAllowed(ThreePid tpid) {
        // We check if the policy allows registration for invites, and if there is an invite for the 3PID
        if (cfg.getPolicy().forInvite() && invMgr.hasInvite(tpid)) {
            log.info("Registration allowed for pending invite");
            return true;
        }

        // The following section deals with patterns which can either be built at startup time, or for each invite at runtime.
        // Registration is a very rare occurrence relatively speaking, so we make the choice to build the patterns each time
        // at runtime to save on RAM.

        Object policy = cfg.getPolicy().getThreepid().get(tpid.getMedium());
        if (Objects.nonNull(policy)) {
            RegisterConfig.ThreepidPolicy tpidPolicy = GsonUtil.get().fromJson(GsonUtil.get().toJson(policy), RegisterConfig.ThreepidPolicy.class);
            log.info("Found registration policy for {}", tpid.getMedium());

            log.info("Processing pattern blacklist");
            for (String pattern : tpidPolicy.getPattern().getBlacklist()) {
                log.info("Processing pattern {}", pattern);

                // We compile the pattern
                Matcher m = Pattern.compile(pattern).matcher(tpid.getAddress());
                if (m.matches()) { // We only care about those who match...
                    log.info("Found matching blacklist entry, denying registration");
                    return false; // ... and get denied as per blacklist
                }
            }

            log.info("Processing pattern whitelist");
            for (String pattern : tpidPolicy.getPattern().getWhitelist()) {
                log.info("Processing pattern {}", pattern);

                // We compile the pattern
                Matcher m = Pattern.compile(pattern).matcher(tpid.getAddress());
                if (m.matches()) { // We only care about those who match...
                    log.info("Found matching whitelist entry, allowing registration");
                    return true; // ... and get accepted as per whitelist
                }
            }
        }

        log.info("Returning default registration policy: {}", cfg.getPolicy().isAllowed());
        return cfg.getPolicy().isAllowed();
    }

}
