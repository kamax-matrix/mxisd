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
import io.kamax.matrix.MatrixID;
import io.kamax.matrix.ThreePid;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.SessionConfig;
import io.kamax.mxisd.exception.NotAllowedException;
import io.kamax.mxisd.exception.NotImplementedException;
import io.kamax.mxisd.exception.SessionNotValidatedException;
import io.kamax.mxisd.exception.SessionUnknownException;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.ThreePidValidation;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
import io.kamax.mxisd.notification.NotificationManager;
import io.kamax.mxisd.storage.IStorage;
import io.kamax.mxisd.storage.dao.IThreePidSessionDao;
import io.kamax.mxisd.threepid.session.ThreePidSession;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static io.kamax.mxisd.config.SessionConfig.Policy.PolicyTemplate;

public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private SessionConfig cfg;
    private MatrixConfig mxCfg;
    private IStorage storage;
    private NotificationManager notifMgr;
    private LookupStrategy lookupMgr;

    // FIXME export into central class, set version
    private CloseableHttpClient client;

    public SessionManager(
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
            throw new NotAllowedException("Validating 3PID is disabled");
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

                String sessionId;
                do {
                    sessionId = Long.toString(System.currentTimeMillis());
                } while (storage.getThreePidSession(sessionId).isPresent());

                String token = RandomStringUtils.randomNumeric(6);
                ThreePidSession session = new ThreePidSession(sessionId, server, tpid, secret, attempt, nextLink, token);
                log.info("Generated new session {} to validate {} from server {}", sessionId, tpid, server);

                storage.insertThreePidSession(session.getDao());
                log.info("Stored session {}", sessionId, tpid, server);

                log.info("Session {} for {}: sending validation notification", sessionId, tpid);
                notifMgr.sendForValidation(session);

                return sessionId;
            }
        }
    }

    public ValidationResult validate(String sid, String secret, String token) {
        ThreePidSession session = getSession(sid, secret);
        log.info("Attempting validation for session {} from {}", session.getId(), session.getServer());

        session.validate(token);
        storage.updateThreePidSession(session.getDao());
        log.info("Session {} has been validated locally", session.getId());

        ValidationResult r = new ValidationResult(session);
        session.getNextLink().ifPresent(r::setNextUrl);
        return r;
    }

    public ThreePidValidation getValidated(String sid, String secret) {
        ThreePidSession session = getSessionIfValidated(sid, secret);
        return new ThreePidValidation(session.getThreePid(), session.getValidationTime());
    }

    public void bind(String sid, String secret, String mxidRaw) {
        // We make sure we have an acceptable User ID
        if (StringUtils.isEmpty(mxidRaw)) {
            throw new IllegalArgumentException("No Matrix User ID provided");
        }

        // We ensure the session was validated
        ThreePidSession session = getSessionIfValidated(sid, secret);

        // We parse the Matrix ID as acceptable
        _MatrixID mxid = MatrixID.asAcceptable(mxidRaw);

        // Only accept binds if the domain matches our own
        if (!StringUtils.equalsIgnoreCase(mxCfg.getDomain(), mxid.getDomain())) {
            throw new NotAllowedException("Only Matrix IDs from domain " + mxCfg + " can be bound");
        }

        log.info("Session {}: Binding of {}:{} to Matrix ID {} is accepted",
                session.getId(), session.getThreePid().getMedium(), session.getThreePid().getAddress(), mxid.getId());
    }

    public void unbind(JsonObject reqData) {
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

            throw new NotAllowedException("You have attempted to alter 3PID bindings, which can only be done by the 3PID owner directly. " +
                    "We have informed the 3PID owner of your fraudulent attempt.");
        }

        log.info("Denying unbind request as the endpoint is not defined in the spec.");
        throw new NotAllowedException(499, "This endpoint does not exist in the spec and therefore is not supported.");
    }

}
