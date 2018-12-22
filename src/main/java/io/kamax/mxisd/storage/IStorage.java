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

package io.kamax.mxisd.storage;

import io.kamax.matrix.ThreePid;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import io.kamax.mxisd.storage.dao.IThreePidSessionDao;
import io.kamax.mxisd.storage.ormlite.dao.ASTransactionDao;
import io.kamax.mxisd.storage.ormlite.dao.ThreePidInviteIO;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface IStorage {

    Collection<ThreePidInviteIO> getInvites();

    void insertInvite(IThreePidInviteReply data);

    void deleteInvite(String id);

    Optional<IThreePidSessionDao> getThreePidSession(String sid);

    Optional<IThreePidSessionDao> findThreePidSession(ThreePid tpid, String secret);

    void insertThreePidSession(IThreePidSessionDao session);

    void updateThreePidSession(IThreePidSessionDao session);

    void insertTransactionResult(String localpart, String txnId, Instant completion, String response);

    Optional<ASTransactionDao> getTransactionResult(String localpart, String txnId);

}
