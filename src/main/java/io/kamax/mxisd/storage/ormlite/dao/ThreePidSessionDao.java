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

package io.kamax.mxisd.storage.ormlite.dao;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import io.kamax.matrix.ThreePid;
import io.kamax.mxisd.storage.dao.IThreePidSessionDao;

@DatabaseTable(tableName = "session_3pid")
public class ThreePidSessionDao implements IThreePidSessionDao {

    @DatabaseField(id = true)
    private String id;

    @DatabaseField(canBeNull = false)
    private long creationTime;

    @DatabaseField(canBeNull = false)
    private String server;

    @DatabaseField(canBeNull = false)
    private String medium;

    @DatabaseField(canBeNull = false)
    private String address;

    @DatabaseField(canBeNull = false)
    private String secret;

    @DatabaseField(canBeNull = false)
    private int attempt;

    @DatabaseField
    private String nextLink;

    @DatabaseField(canBeNull = false)
    private String token;

    @DatabaseField
    private boolean validated;

    @DatabaseField
    private long validationTime;

    @DatabaseField(canBeNull = false)
    private boolean isRemote;

    @DatabaseField
    private String remoteServer;

    @DatabaseField
    private String remoteId;

    @DatabaseField
    private String remoteSecret;

    @DatabaseField
    private Integer remoteAttempt;

    @DatabaseField(canBeNull = false)
    private boolean isRemoteValidated;

    public ThreePidSessionDao() {
        // Needed for ORMLite
    }

    public ThreePidSessionDao(IThreePidSessionDao session) {
        setId(session.getId());
        setCreationTime(session.getCreationTime());
        setServer(session.getServer());
        setMedium(session.getMedium());
        setAddress(session.getAddress());
        setSecret(session.getSecret());
        setAttempt(session.getAttempt());
        setNextLink(session.getNextLink());
        setToken(session.getToken());
        setValidated(session.getValidated());
        setValidationTime(session.getValidationTime());
        setRemote(session.isRemote());
        setRemoteServer(session.getRemoteServer());
        setRemoteId(session.getRemoteId());
        setRemoteSecret(session.getRemoteSecret());
        setRemoteAttempt(session.getRemoteAttempt());
        setRemoteValidated(session.isRemoteValidated());
    }

    public ThreePidSessionDao(ThreePid tpid, String secret) {
        setMedium(tpid.getMedium());
        setAddress(tpid.getAddress());
        setSecret(secret);
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    @Override
    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    @Override
    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    @Override
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Override
    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    @Override
    public String getNextLink() {
        return nextLink;
    }

    public void setNextLink(String nextLink) {
        this.nextLink = nextLink;
    }

    @Override
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean getValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    @Override
    public long getValidationTime() {
        return validationTime;
    }

    @Override
    public boolean isRemote() {
        return isRemote;
    }

    public void setRemote(boolean remote) {
        isRemote = remote;
    }

    @Override
    public String getRemoteServer() {
        return remoteServer;
    }

    public void setRemoteServer(String remoteServer) {
        this.remoteServer = remoteServer;
    }

    @Override
    public String getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(String remoteId) {
        this.remoteId = remoteId;
    }

    @Override
    public String getRemoteSecret() {
        return remoteSecret;
    }

    public void setRemoteSecret(String remoteSecret) {
        this.remoteSecret = remoteSecret;
    }

    @Override
    public int getRemoteAttempt() {
        return remoteAttempt;
    }

    @Override
    public boolean isRemoteValidated() {
        return isRemoteValidated;
    }

    public void setRemoteValidated(boolean remoteValidated) {
        isRemoteValidated = remoteValidated;
    }

    public void setRemoteAttempt(int remoteAttempt) {
        this.remoteAttempt = remoteAttempt;
    }

    public void setValidationTime(long validationTime) {
        this.validationTime = validationTime;
    }

}
