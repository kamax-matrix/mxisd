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
import io.kamax.mxisd.exception.BadRequestException;
import io.kamax.mxisd.exception.NotAllowedException;
import io.kamax.mxisd.exception.SessionNotValidatedException;
import io.kamax.mxisd.exception.SessionUnknownException;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidValidation;
import io.kamax.mxisd.notification.NotificationManager;
import io.kamax.mxisd.storage.IStorage;
import io.kamax.mxisd.storage.dao.IThreePidSessionDao;
import io.kamax.mxisd.threepid.session.ThreePidSession;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
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

    public SessionManager(
            SessionConfig cfg,
            MatrixConfig mxCfg,
            IStorage storage,
            NotificationManager notifMgr
    ) {
        this.cfg = cfg;
        this.mxCfg = mxCfg;
        this.storage = storage;
        this.notifMgr = notifMgr;
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
                log.info("Stored session {}", sessionId);

                log.info("Session {} for {}: sending validation notification", sessionId, tpid);
                notifMgr.sendForValidation(session);

                return sessionId;
            }
        }
    }

    public ValidationResult validate(String sid, String secret, String token) {
        log.info("Validating session {}", sid);
        ThreePidSession session = getSession(sid, secret);
        log.info("Session {} is from {}", session.getId(), session.getServer());

        session.validate(token);
        storage.updateThreePidSession(session.getDao());
        log.info("Session {} has been validated", session.getId());

        ValidationResult r = new ValidationResult(session);
        session.getNextLink().ifPresent(r::setNextUrl);
        return r;
    }

    public ThreePidValidation getValidated(String sid, String secret) {
        ThreePidSession session = getSessionIfValidated(sid, secret);
        return new ThreePidValidation(session.getThreePid(), session.getValidationTime());
    }

    public SingleLookupReply bind(String sid, String secret, String mxidRaw) {
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
            throw new NotAllowedException("Only Matrix IDs from domain " + mxCfg.getDomain() + " can be bound");
        }

        log.info("Session {}: Binding of {}:{} to Matrix ID {} is accepted",
                session.getId(), session.getThreePid().getMedium(), session.getThreePid().getAddress(), mxid.getId());

        SingleLookupRequest request = new SingleLookupRequest();
        request.setType(session.getThreePid().getMedium());
        request.setThreePid(session.getThreePid().getAddress());
        return new SingleLookupReply(request, mxid);
    }

    public void unbind(JsonObject reqData) {
        _MatrixID mxid;
        try {
            mxid = MatrixID.asAcceptable(GsonUtil.getStringOrThrow(reqData, "mxid"));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        String sid = GsonUtil.getStringOrNull(reqData, "sid");
        String secret = GsonUtil.getStringOrNull(reqData, "client_secret");
        ThreePid tpid = GsonUtil.get().fromJson(GsonUtil.getObj(reqData, "threepid"), ThreePid.class);

        // We ensure the session was validated
        ThreePidSession session = getSessionIfValidated(sid, secret);

        // As per spec, we can only allow if the provided 3PID matches the validated session
        if (!session.getThreePid().equals(tpid)) {
            throw new BadRequestException("3PID to unbind does not match the one from the validated session");
        }

        // We only allow unbind for the domain we manage, mirroring bind
        if (!StringUtils.equalsIgnoreCase(mxCfg.getDomain(), mxid.getDomain())) {
            throw new NotAllowedException("Only Matrix IDs from domain " + mxCfg.getDomain() + " can be unbound");
        }

        log.info("Session {}: Unbinding of {}:{} to Matrix ID {} is accepted",
                session.getId(), session.getThreePid().getMedium(), session.getThreePid().getAddress(), mxid.getId());
    }

}
