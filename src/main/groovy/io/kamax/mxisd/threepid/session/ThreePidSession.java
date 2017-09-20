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

package io.kamax.mxisd.threepid.session;

import io.kamax.mxisd.ThreePid;
import io.kamax.mxisd.exception.BadRequestException;
import io.kamax.mxisd.exception.InvalidCredentialsException;
import io.kamax.mxisd.storage.dao.IThreePidSessionDao;
import org.apache.commons.lang.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class ThreePidSession implements IThreePidSession {

    private String id;
    private Instant timestamp;
    private String server;
    private ThreePid tPid;
    private String secret;
    private String nextLink;
    private String token;
    private int attempt;
    private Instant validationTimestamp;
    private boolean isValidated;

    public ThreePidSession(IThreePidSessionDao dao) {
        this(
                dao.getId(),
                dao.getServer(),
                new ThreePid(dao.getMedium(), dao.getAddress()),
                dao.getSecret(),
                dao.getAttempt(),
                dao.getNextLink(),
                dao.getToken()
        );
    }

    public ThreePidSession(String id, String server, ThreePid tPid, String secret, int attempt, String nextLink, String token) {
        this.id = id;
        this.server = server;
        this.tPid = new ThreePid(tPid);
        this.secret = secret;
        this.attempt = attempt;
        this.nextLink = nextLink;
        this.token = token;

        this.timestamp = Instant.now();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Instant getCreationTime() {
        return timestamp;
    }

    @Override
    public String getServer() {
        return server;
    }

    @Override
    public ThreePid getThreePid() {
        return tPid;
    }

    public String getSecret() {
        return secret;
    }

    @Override
    public int getAttempt() {
        return attempt;
    }

    @Override
    public void increaseAttempt() {
        attempt++;
    }

    @Override
    public Optional<String> getNextLink() {
        return Optional.ofNullable(nextLink);
    }

    @Override
    public String getToken() {
        return token;
    }

    public synchronized void setAttempt(int attempt) {
        if (isValidated()) {
            throw new IllegalStateException();
        }

        this.attempt = attempt;
    }

    @Override
    public Instant getValidationTime() {
        return validationTimestamp;
    }

    @Override
    public boolean isValidated() {
        return isValidated;
    }

    public synchronized void validate(String token) {
        if (Instant.now().minus(24, ChronoUnit.HOURS).isAfter(getCreationTime())) {
            throw new BadRequestException("Session " + getId() + " has expired");
        }

        if (!StringUtils.equals(this.token, token)) {
            throw new InvalidCredentialsException();
        }

        if (isValidated()) {
            return;
        }

        validationTimestamp = Instant.now();
        isValidated = true;
    }

    public IThreePidSessionDao getDao() {
        return new IThreePidSessionDao() {

            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getServer() {
                return server;
            }

            @Override
            public String getMedium() {
                return tPid.getMedium();
            }

            @Override
            public String getAddress() {
                return tPid.getAddress();
            }

            @Override
            public String getSecret() {
                return secret;
            }

            @Override
            public int getAttempt() {
                return attempt;
            }

            @Override
            public String getNextLink() {
                return nextLink;
            }

            @Override
            public String getToken() {
                return token;
            }

        };
    }

}
