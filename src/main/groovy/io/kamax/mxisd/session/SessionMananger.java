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

package io.kamax.mxisd.session;

import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.ThreePid;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.SessionConfig;
import io.kamax.mxisd.exception.InvalidCredentialsException;
import io.kamax.mxisd.exception.NotAllowedException;
import io.kamax.mxisd.exception.SessionNotValidatedException;
import io.kamax.mxisd.lookup.ThreePidValidation;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
import io.kamax.mxisd.notification.NotificationManager;
import io.kamax.mxisd.storage.IStorage;
import io.kamax.mxisd.storage.dao.IThreePidSessionDao;
import io.kamax.mxisd.threepid.session.ThreePidSession;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SessionMananger {

    private Logger log = LoggerFactory.getLogger(SessionMananger.class);

    private SessionConfig cfg;
    private IStorage storage;
    private LookupStrategy lookup;
    private NotificationManager notifMgr;

    @Autowired
    public SessionMananger(SessionConfig cfg, MatrixConfig mxCfg, IStorage storage, LookupStrategy lookup, NotificationManager notifMgr) {
        this.cfg = cfg;
        this.storage = storage;
        this.lookup = lookup;
        this.notifMgr = notifMgr;
    }

    private boolean isLocal(ThreePid tpid) {
        if (!ThreePidMedium.Email.is(tpid.getMedium())) { // We can only handle E-mails for now
            return false;
        }

        String domain = tpid.getAddress().split("@")[1];
        return StringUtils.equalsIgnoreCase(cfg.getMatrixCfg().getDomain(), domain);
    }

    private boolean isKnownLocal(ThreePid tpid) {
        return lookup.findLocal(tpid.getMedium(), tpid.getAddress()).isPresent();
    }

    private ThreePidSession getSession(String sid, String secret) {
        Optional<IThreePidSessionDao> dao = storage.getThreePidSession(sid);
        if (!dao.isPresent() || !StringUtils.equals(dao.get().getSecret(), secret)) {
            throw new InvalidCredentialsException();
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
        SessionConfig.Policy.PolicyTemplate policy = cfg.getPolicy().getValidation();
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
                SessionConfig.Policy.PolicyTemplate.PolicySource policySource = policy.forIf(isLocal);
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
                    notifMgr.sendforRemoteValidation(session);
                }

                storage.insertThreePidSession(session.getDao());
                log.info("Stored session {}", sessionId, tpid, server);

                return sessionId;
            }
        }
    }

    public Optional<String> validate(String sid, String secret, String token) {
        ThreePidSession session = getSession(sid, secret);
        log.info("Attempting validation for session {} from {}", session.getId(), session.getServer());
        session.validate(token);
        storage.updateThreePidSession(session.getDao());
        log.info("Session {} has been validated", session.getId());
        return session.getNextLink();
    }

    public ThreePidValidation getValidated(String sid, String secret) {
        ThreePidSession session = getSessionIfValidated(sid, secret);
        return new ThreePidValidation(session.getThreePid(), session.getValidationTime());
    }

    public void bind(String sid, String secret, String mxid) {
        ThreePidSession session = getSessionIfValidated(sid, secret);
        log.info("Accepting bind of {} on session {} from server {}", mxid, session.getId(), session.getServer());
        // TODO perform this if request was proxied
    }

}
