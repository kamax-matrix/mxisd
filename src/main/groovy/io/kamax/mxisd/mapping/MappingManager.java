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

package io.kamax.mxisd.mapping;

import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.ThreePid;
import io.kamax.mxisd.exception.InvalidCredentialsException;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.ThreePidValidation;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
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
import java.util.UUID;

@Component
public class MappingManager {

    private Logger log = LoggerFactory.getLogger(MappingManager.class);

    private IStorage storage;
    private LookupStrategy lookup;

    @Autowired
    public MappingManager(IStorage storage, LookupStrategy lookup) {
        this.storage = storage;
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
            throw new IllegalStateException("Session " + sid + " has not been validated");
        }
        return session;
    }

    public String create(String server, ThreePid tpid, String secret, int attempt, String nextLink) {
        synchronized (this) {
            log.info("Server {} is asking to create session for {} (Attempt #{}) - Next link: {}", server, tpid, attempt, nextLink);
            Optional<IThreePidSessionDao> dao = storage.findThreePidSession(tpid, secret);
            if (dao.isPresent()) {
                ThreePidSession session = new ThreePidSession(dao.get());
                log.info("We already have a session for {}: {}", tpid, session.getId());
                if (session.getAttempt() < attempt) {
                    log.info("Received attempt {} is greater than stored attempt {}, sending validation communication", attempt, session.getAttempt());
                    // TODO send via connector
                    session.increaseAttempt();
                    storage.updateThreePidSession(session.getDao());
                }

                return session.getId();
            } else {
                log.info("No existing session for {}", tpid);
                String sessionId;
                do {
                    sessionId = UUID.randomUUID().toString().replace("-", "");
                } while (storage.getThreePidSession(sessionId).isPresent());

                String token = RandomStringUtils.randomNumeric(6);
                ThreePidSession session = new ThreePidSession(sessionId, server, tpid, secret, attempt, nextLink, token);
                log.info("Generated new session {} to validate {} from server {}", sessionId, tpid, server);

                // TODO send via connector
                // log.info("Sent validation notification to {}", tpid);

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
        log.info("Attempting bind of {} on session {} from server {}", mxid, session.getId(), session.getServer());

        // We lookup if the 3PID is already known locally.
        // If it is, we do not need to process any further as it is already bound.
        Optional<SingleLookupReply> rLocal = lookup.findLocal(session.getThreePid().getMedium(), session.getThreePid().getAddress());
        boolean knownLocal = rLocal.isPresent() && StringUtils.equals(rLocal.get().getMxid().getId(), mxid);
        log.info("Mapping {} -> {} is " + (knownLocal ? "already" : "not") + " known locally", mxid, session.getThreePid());

        // MXID is not known locally, checking remotely
        Optional<SingleLookupReply> rRemote = lookup.findRemote(session.getThreePid().getMedium(), session.getThreePid().getAddress());
        boolean knownRemote = rRemote.isPresent() && StringUtils.equals(rRemote.get().getMxid().getId(), mxid);
        log.info("Mapping {} -> {} is " + (knownRemote ? "already" : "not") + " known remotely", mxid, session.getThreePid());

        boolean isLocalDomain = false;
        if (ThreePidMedium.Email.is(session.getThreePid().getMedium())) {
            // TODO
            // 1. Extract domain from email
            // 2. set isLocalDomain
            isLocalDomain = session.getThreePid().getAddress().isEmpty(); // FIXME only for testing
        }
        if (knownRemote) {
            if (isLocalDomain && !knownLocal) {
                log.warn("Mapping {} -> {} is not known locally but is about a local domain!");
            }

            log.info("No further action needed for Mapping {} -> {}");
            return;
        }

        // This might need a configuration by medium type?
        if (knownLocal) { // 3PID is ony known local
            if (isLocalDomain) {
                // TODO
                // 1. Check if global publishing is enabled, allowed and offered. If one is no, return.
                // 2. Publish globally
            }

            if (System.currentTimeMillis() % 2 == 0) {
                // TODO
                // 1. Check if configured to publish globally non-local domain. If no, return
            }

            // TODO
            // Proxy to configurable IS, by default Matrix.org
            //
            // Separate workflow, if user accepts to publish globally
            // 1. display page to the user that it is waiting for the confirmation
            // 2. call mxisd-specific endpoint to publish globally
            // 3. check regularly on client page for a binding
            // 4. when found, show page "Done globally!"
        } else {
            if (isLocalDomain) { // 3PID is not known anywhere but is a local domain
                // TODO
                // check if config says this should fail or silently accept.
                // Required to silently accept if the backend is synapse itself.
            } else { // 3PID is not known anywhere and is remote
                // TODO
                // Proxy to configurable IS, by default Matrix.org
            }
        }
    }

}
