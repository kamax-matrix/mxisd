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

package io.kamax.mxisd.session;

import com.google.gson.JsonObject;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix.ThreePid;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.SessionConfig;
import io.kamax.mxisd.exception.*;
import io.kamax.mxisd.http.io.identity.RequestTokenResponse;
import io.kamax.mxisd.http.undertow.handler.identity.v1.RemoteIdentityAPIv1;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.ThreePidValidation;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
import io.kamax.mxisd.matrix.IdentityServerUtils;
import io.kamax.mxisd.notification.NotificationManager;
import io.kamax.mxisd.storage.IStorage;
import io.kamax.mxisd.storage.dao.IThreePidSessionDao;
import io.kamax.mxisd.threepid.session.IThreePidSession;
import io.kamax.mxisd.threepid.session.ThreePidSession;
import io.kamax.mxisd.util.GsonParser;
import io.kamax.mxisd.util.RestClientUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static io.kamax.mxisd.config.SessionConfig.Policy.PolicyTemplate;
import static io.kamax.mxisd.config.SessionConfig.Policy.PolicyTemplate.PolicySource;

public class SessionMananger {

    private transient final Logger log = LoggerFactory.getLogger(SessionMananger.class);

    private SessionConfig cfg;
    private MatrixConfig mxCfg;
    private IStorage storage;
    private NotificationManager notifMgr;
    private LookupStrategy lookupMgr;

    private GsonParser parser = new GsonParser();
    private PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance(); // FIXME refactor for sessions handling their own stuff

    // FIXME export into central class, set version
    private CloseableHttpClient client;

    public SessionMananger(
            SessionConfig cfg,
            MatrixConfig mxCfg,
            IStorage storage,
            NotificationManager notifMgr,
            LookupStrategy lookupMgr,
            CloseableHttpClient client
    ) {
        this.cfg = cfg;
        this.mxCfg = mxCfg;
        this.storage = storage;
        this.notifMgr = notifMgr;
        this.lookupMgr = lookupMgr;
        this.client = client;
    }

    private boolean isLocal(ThreePid tpid) {
        if (!ThreePidMedium.Email.is(tpid.getMedium())) { // We can only handle E-mails for now
            return false;
        }

        String domain = tpid.getAddress().split("@")[1];
        return StringUtils.equalsIgnoreCase(mxCfg.getDomain(), domain);
    }

    private ThreePidSession getSession(String sid, String secret) {
        Optional<IThreePidSessionDao> dao = storage.getThreePidSession(sid);
        if (!dao.isPresent() || !StringUtils.equals(dao.get().getSecret(), secret)) {
            throw new SessionUnknownException();
        }

        return new ThreePidSession(dao.get());
    }

    private ThreePidSession getSessionIfValidated(String sid, String secret) {
        ThreePidSession session = getSession(sid, secret);
        if (!session.isValidated()) {
            throw new SessionNotValidatedException();
        }
        return session;
    }

    public String create(String server, ThreePid tpid, String secret, int attempt, String nextLink) {
        PolicyTemplate policy = cfg.getPolicy().getValidation();
        if (!policy.isEnabled()) {
            throw new NotAllowedException("Validating 3PID is disabled globally");
        }

        synchronized (this) {
            log.info("Server {} is asking to create session for {} (Attempt #{}) - Next link: {}", server, tpid, attempt, nextLink);
            Optional<IThreePidSessionDao> dao = storage.findThreePidSession(tpid, secret);
            if (dao.isPresent()) {
                ThreePidSession session = new ThreePidSession(dao.get());
                log.info("We already have a session for {}: {}", tpid, session.getId());
                if (session.getAttempt() < attempt) {
                    log.info("Received attempt {} is greater than stored attempt {}, sending validation communication", attempt, session.getAttempt());
                    notifMgr.sendForValidation(session);
                    log.info("Sent validation notification to {}", tpid);
                    session.increaseAttempt();
                    storage.updateThreePidSession(session.getDao());
                }

                return session.getId();
            } else {
                log.info("No existing session for {}", tpid);

                boolean isLocal = isLocal(tpid);
                log.info("Is 3PID bound to local domain? {}", isLocal);

                // This might need a configuration by medium type?
                PolicySource policySource = policy.forIf(isLocal);
                if (!policySource.isEnabled() || (!policySource.toLocal() && !policySource.toRemote())) {
                    log.info("Session for {}: cancelled due to policy", tpid);
                    throw new NotAllowedException("Validating " + (isLocal ? "local" : "remote") + " 3PID is not allowed");
                }

                String sessionId;
                do {
                    sessionId = Long.toString(System.currentTimeMillis());
                } while (storage.getThreePidSession(sessionId).isPresent());

                String token = RandomStringUtils.randomNumeric(6);
                ThreePidSession session = new ThreePidSession(sessionId, server, tpid, secret, attempt, nextLink, token);
                log.info("Generated new session {} to validate {} from server {}", sessionId, tpid, server);

                // This might need a configuration by medium type?
                if (policySource.toLocal()) {
                    log.info("Session {} for {}: sending local validation notification", sessionId, tpid);
                    notifMgr.sendForValidation(session);
                } else {
                    log.info("Session {} for {}: sending remote-only validation notification", sessionId, tpid);
                    notifMgr.sendForRemoteValidation(session);
                }

                storage.insertThreePidSession(session.getDao());
                log.info("Stored session {}", sessionId, tpid, server);

                return sessionId;
            }
        }
    }

    public ValidationResult validate(String sid, String secret, String token) {
        ThreePidSession session = getSession(sid, secret);
        log.info("Attempting validation for session {} from {}", session.getId(), session.getServer());

        boolean isLocal = isLocal(session.getThreePid());
        PolicySource policy = cfg.getPolicy().getValidation().forIf(isLocal);
        if (!policy.isEnabled()) {
            throw new NotAllowedException("Validating " + (isLocal ? "local" : "remote") + " 3PID is not allowed");
        }

        if (ThreePidMedium.PhoneNumber.is(session.getThreePid().getMedium()) && session.isValidated() && session.isRemote()) {
            submitRemote(session, token);
            session.validateRemote();
            return new ValidationResult(session, false);
        }

        session.validate(token);
        storage.updateThreePidSession(session.getDao());
        log.info("Session {} has been validated locally", session.getId());

        if (ThreePidMedium.PhoneNumber.is(session.getThreePid().getMedium()) && session.isValidated() && policy.toRemote()) {
            createRemote(sid, secret);
            // FIXME make the message configurable/customizable (templates?)
            throw new MessageForClientException("You will receive a NEW code from another number. Enter it below");
        }

        // FIXME definitely doable in a nicer way
        ValidationResult r = new ValidationResult(session, policy.toRemote());
        if (!policy.toLocal()) {
            r.setNextUrl(RemoteIdentityAPIv1.getRequestToken(sid, secret));
        } else {
            session.getNextLink().ifPresent(r::setNextUrl);
        }
        return r;
    }

    public ThreePidValidation getValidated(String sid, String secret) {
        ThreePidSession session = getSessionIfValidated(sid, secret);
        return new ThreePidValidation(session.getThreePid(), session.getValidationTime());
    }

    public void bind(String sid, String secret, String mxidRaw) {
        if (StringUtils.isEmpty(mxidRaw)) {
            throw new IllegalArgumentException("No Matrix User ID provided");
        }

        _MatrixID mxid = MatrixID.asAcceptable(mxidRaw);
        ThreePidSession session = getSessionIfValidated(sid, secret);

        if (!session.isRemote()) {
            log.info("Session {} for {}: MXID {} was bound locally", sid, session.getThreePid(), mxid);
            return;
        }

        log.info("Session {} for {}: MXID {} bind is remote", sid, session.getThreePid(), mxid);
        if (!session.isRemoteValidated()) {
            log.error("Session {} for {}: Not validated remotely", sid, session.getThreePid());
            throw new SessionNotValidatedException();
        }

        log.info("Session {} for {}: Performing remote bind", sid, session.getThreePid());

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(
                Arrays.asList(
                        new BasicNameValuePair("sid", session.getRemoteId()),
                        new BasicNameValuePair("client_secret", session.getRemoteSecret()),
                        new BasicNameValuePair("mxid", mxid.getId())
                ), StandardCharsets.UTF_8);
        HttpPost bindReq = new HttpPost(session.getRemoteServer() + "/_matrix/identity/api/v1/3pid/bind");
        bindReq.setEntity(entity);

        try (CloseableHttpResponse response = client.execute(bindReq)) {
            int status = response.getStatusLine().getStatusCode();
            if (status < 200 || status >= 300) {
                String body = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                log.error("Session {} for {}: Remote IS {} failed when trying to bind {} for remote session {}\n{}",
                        sid, session.getThreePid(), session.getRemoteServer(), mxid, session.getRemoteId(), body);
                throw new RemoteIdentityServerException(body);
            }

            log.error("Session {} for {}: MXID {} was bound remotely", sid, session.getThreePid(), mxid);
        } catch (IOException e) {
            log.error("Session {} for {}: I/O Error when trying to bind mxid {}", sid, session.getThreePid(), mxid);
            throw new RemoteIdentityServerException(e.getMessage());
        }
    }

    public void unbind(JsonObject reqData) {
        // TODO also check for HS header to know which domain attempting the unbind
        if (reqData.entrySet().size() == 2 && reqData.has("mxid") && reqData.has("threepid")) {
            /* This is a HS request to remove a 3PID and is considered:
             * - An attack on user privacy
             * - A baffling spec breakage requiring IS and HS 3PID info to be independent [1]
             * - A baffling spec breakage that 3PID (un)bind is only one way [2]
             *
             * Given the lack of response on our extensive feedback on the proposal [3] which has not landed in the spec yet [4],
             * We'll be denying such unbind requests and will inform users using their 3PID that a fraudulent attempt of
             * removing their 3PID binding has been attempted and blocked.
             *
             * [1]: https://matrix.org/docs/spec/client_server/r0.4.0.html#adding-account-administrative-contact-information
             * [2]: https://matrix.org/docs/spec/identity_service/r0.1.0.html#privacy
             * [3]: https://docs.google.com/document/d/135g2muVxmuml0iUnLoTZxk8M2ZSt3kJzg81chGh51yg/edit
             * [4]: https://github.com/matrix-org/matrix-doc/issues/1194
             */

            log.warn("A remote host attempted to unbind without proper authorization. Request was denied");

            if (!cfg.getPolicy().getUnbind().getFraudulent().getSendWarning()) {
                log.info("Not sending notification to 3PID owner as per configuration");
            } else {
                log.info("Sending notification to 3PID owner as per configuration");

                ThreePid tpid = GsonUtil.get().fromJson(GsonUtil.getObj(reqData, "threepid"), ThreePid.class);
                Optional<SingleLookupReply> lookup = lookupMgr.findLocal(tpid.getMedium(), tpid.getAddress());
                if (!lookup.isPresent()) {
                    log.info("No 3PID owner found, not sending any notification");
                } else {
                    log.info("3PID owner found, sending notification");
                    try {
                        notifMgr.sendForFraudulentUnbind(tpid);
                        log.info("Notification sent");
                    } catch (NotImplementedException e) {
                        log.warn("Unable to send notification: {}", e.getMessage());
                    } catch (RuntimeException e) {
                        log.warn("Unable to send notification due to unknown error. See stacktrace below", e);
                    }
                }
            }
        }

        log.info("Denying request");
        throw new NotAllowedException("You have attempted to alter 3PID bindings, which can only be done by the 3PID owner directly. " +
                "We have informed the 3PID owner of your fraudulent attempt.");
    }

    public IThreePidSession createRemote(String sid, String secret) {
        ThreePidSession session = getSessionIfValidated(sid, secret);
        log.info("Creating remote 3PID session for {} with local session [{}] to {}", session.getThreePid(), sid);

        boolean isLocal = isLocal(session.getThreePid());
        PolicySource policy = cfg.getPolicy().getValidation().forIf(isLocal);
        if (!policy.isEnabled() || !policy.toRemote()) {
            throw new NotAllowedException("Validating " + (isLocal ? "local" : "remote") + " 3PID is not allowed");
        }
        log.info("Remote 3PID is allowed by policy");

        List<String> servers = mxCfg.getIdentity().getServers(policy.getToRemote().getServer());
        if (servers.isEmpty()) {
            throw new FeatureNotAvailable("Remote 3PID sessions are enabled but server list is " +
                    "misconstrued (invalid ID or empty list");
        }

        String is = servers.get(0);
        String url = IdentityServerUtils.findIsUrlForDomain(is).orElse(is);
        log.info("Will use IS endpoint {}", url);

        String remoteSecret = session.isRemote() ? session.getRemoteSecret() : RandomStringUtils.randomAlphanumeric(16);

        JsonObject body = new JsonObject();
        body.addProperty("client_secret", remoteSecret);
        body.addProperty(session.getThreePid().getMedium(), session.getThreePid().getAddress());
        body.addProperty("send_attempt", session.increaseAndGetRemoteAttempt());
        if (ThreePidMedium.PhoneNumber.is(session.getThreePid().getMedium())) {
            try {
                Phonenumber.PhoneNumber msisdn = phoneUtil.parse("+" + session.getThreePid().getAddress(), null);
                String country = phoneUtil.getRegionCodeForNumber(msisdn).toUpperCase();
                body.addProperty("phone_number", phoneUtil.format(msisdn, PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
                body.addProperty("country", country);
            } catch (NumberParseException e) {
                throw new InternalServerError(e);
            }
        } else {
            body.addProperty(session.getThreePid().getMedium(), session.getThreePid().getAddress());
        }

        log.info("Requesting remote session with attempt {}", session.getRemoteAttempt());
        HttpPost tokenReq = RestClientUtils.post(url + "/_matrix/identity/api/v1/validate/" + session.getThreePid().getMedium() + "/requestToken", body);
        try (CloseableHttpResponse response = client.execute(tokenReq)) {
            int status = response.getStatusLine().getStatusCode();
            if (status < 200 || status >= 300) {
                JsonObject obj = parser.parseOptional(response).orElseThrow(() -> new RemoteIdentityServerException("Status " + status));
                throw new RemoteIdentityServerException(obj.get("errcode").getAsString() + ": " + obj.get("error").getAsString());
            }

            RequestTokenResponse data = new GsonParser().parse(response, RequestTokenResponse.class);
            log.info("Remote Session ID: {}", data.getSid());

            session.setRemoteData(url, data.getSid(), remoteSecret, 1);
            storage.updateThreePidSession(session.getDao());
            log.info("Updated Session {} with remote data", sid);

            return session;
        } catch (IOException e) {
            log.warn("Failed to create remote session with {} for {}: {}", url, session.getThreePid(), e.getMessage());
            throw new RemoteIdentityServerException(e.getMessage());
        }
    }

    private void submitRemote(ThreePidSession session, String token) {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(
                Arrays.asList(
                        new BasicNameValuePair("sid", session.getRemoteId()),
                        new BasicNameValuePair("client_secret", session.getRemoteSecret()),
                        new BasicNameValuePair("token", token)
                ), StandardCharsets.UTF_8);
        HttpPost submitReq = new HttpPost(session.getRemoteServer() + "/_matrix/identity/api/v1/submitToken");
        submitReq.setEntity(entity);

        try (CloseableHttpResponse response = client.execute(submitReq)) {
            JsonObject o = new GsonParser().parse(response.getEntity().getContent());
            if (!o.has("success") || !o.get("success").getAsBoolean()) {
                String errcode = o.get("errcode").getAsString();
                throw new RemoteIdentityServerException(errcode + ": " + o.get("error").getAsString());
            }

            log.info("Successfully submitted validation token for {} to {}", session.getThreePid(), session.getRemoteServer());
        } catch (IOException e) {
            throw new RemoteIdentityServerException(e.getMessage());
        }
    }

    public void validateRemote(String sid, String secret) {
        ThreePidSession session = getSessionIfValidated(sid, secret);
        if (!session.isRemote()) {
            throw new NotAllowedException("Cannot remotely validate a local session");
        }

        log.info("Session {} for {}: Validating remote 3PID session {} on {}", sid, session.getThreePid(), session.getRemoteId(), session.getRemoteServer());
        if (session.isRemoteValidated()) {
            log.info("Session {} for {}: Already remotely validated", sid, session.getThreePid());
            return;
        }

        HttpGet validateReq = new HttpGet(session.getRemoteServer() + "/_matrix/identity/api/v1/3pid/getValidated3pid?sid=" + session.getRemoteId() + "&client_secret=" + session.getRemoteSecret());
        try (CloseableHttpResponse response = client.execute(validateReq)) {
            int status = response.getStatusLine().getStatusCode();
            if (status < 200 || status >= 300) {
                throw new RemoteIdentityServerException("Remote identity server returned with status " + status);
            }

            JsonObject o = new GsonParser().parse(response.getEntity().getContent());
            if (o.has("errcode")) {
                String errcode = o.get("errcode").getAsString();
                if (StringUtils.equals("M_SESSION_NOT_VALIDATED", errcode)) {
                    throw new SessionNotValidatedException();
                } else if (StringUtils.equals("M_NO_VALID_SESSION", errcode)) {
                    throw new SessionUnknownException();
                } else {
                    throw new RemoteIdentityServerException("Unknown error while validating Remote 3PID session: " + errcode + " - " + o.get("error").getAsString());
                }
            }

            if (o.has("validated_at")) {
                ThreePid remoteThreePid = new ThreePid(o.get("medium").getAsString(), o.get("address").getAsString());
                if (!session.getThreePid().equals(remoteThreePid)) { // sanity check
                    throw new InternalServerError("Local 3PID " + session.getThreePid() + " and remote 3PID " + remoteThreePid + " do not match for session " + session.getId());
                }

                log.info("Session {} for {}: Remotely validated successfully", sid, session.getThreePid());
                session.validateRemote();
                storage.updateThreePidSession(session.getDao());
                log.info("Session {} was updated in storage", sid);
            }
        } catch (IOException e) {
            log.warn("Session {} for {}: Failed to validated remotely on {}: {}", sid, session.getThreePid(), session.getRemoteServer(), e.getMessage());
            throw new RemoteIdentityServerException(e.getMessage());
        }
    }

}
