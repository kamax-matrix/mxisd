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

package io.kamax.mxisd.storage.ormlite;

import com.j256.ormlite.dao.CloseableWrappedIterable;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import io.kamax.mxisd.ThreePid;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import io.kamax.mxisd.storage.IStorage;
import io.kamax.mxisd.storage.dao.IThreePidSessionDao;
import io.kamax.mxisd.storage.ormlite.dao.ThreePidSessionDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class OrmLiteSqliteStorage implements IStorage {

    private Logger log = LoggerFactory.getLogger(OrmLiteSqliteStorage.class);

    @FunctionalInterface
    private interface Getter<T> {

        T get() throws SQLException, IOException;

    }

    @FunctionalInterface
    private interface Doer {

        void run() throws SQLException, IOException;

    }

    private Dao<ThreePidInviteIO, String> invDao;
    private Dao<ThreePidSessionDao, String> sessionDao;

    OrmLiteSqliteStorage(String path) {
        withCatcher(() -> {
            File parent = new File(path).getParentFile();
            if (!parent.mkdirs() && !parent.isDirectory()) {
                throw new RuntimeException("Unable to create DB parent directory: " + parent);
            }

            ConnectionSource connPool = new JdbcConnectionSource("jdbc:sqlite:" + path);
            invDao = createDaoAndTable(connPool, ThreePidInviteIO.class);
            sessionDao = createDaoAndTable(connPool, ThreePidSessionDao.class);
        });
    }

    private <V, K> Dao<V, K> createDaoAndTable(ConnectionSource connPool, Class<V> c) throws SQLException {
        Dao<V, K> dao = DaoManager.createDao(connPool, c);
        TableUtils.createTableIfNotExists(connPool, c);
        return dao;
    }

    private <T> T withCatcher(Getter<T> g) {
        try {
            return g.get();
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e); // FIXME do better
        }
    }

    private void withCatcher(Doer d) {
        try {
            d.run();
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e); // FIXME do better
        }
    }

    private <T> List<T> forIterable(CloseableWrappedIterable<? extends T> t) {
        return withCatcher(() -> {
            try {
                List<T> ioList = new ArrayList<>();
                t.forEach(ioList::add);
                return ioList;
            } finally {
                t.close();
            }
        });
    }

    @Override
    public Collection<ThreePidInviteIO> getInvites() {
        return forIterable(invDao.getWrappedIterable());
    }

    @Override
    public void insertInvite(IThreePidInviteReply data) {
        withCatcher(() -> {
            int updated = invDao.create(new ThreePidInviteIO(data));
            if (updated != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + updated);
            }
        });
    }

    @Override
    public void deleteInvite(String id) {
        withCatcher(() -> {
            int updated = invDao.deleteById(id);
            if (updated != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + updated);
            }
        });
    }

    @Override
    public Optional<IThreePidSessionDao> getThreePidSession(String sid) {
        return withCatcher(() -> Optional.ofNullable(sessionDao.queryForId(sid)));
    }

    @Override
    public Optional<IThreePidSessionDao> findThreePidSession(ThreePid tpid, String secret) {
        return withCatcher(() -> {
            List<ThreePidSessionDao> daoList = sessionDao.queryForMatchingArgs(new ThreePidSessionDao(tpid, secret));
            if (daoList.size() > 1) {
                log.error("Lookup for 3PID Session {}:{} returned more than one result");
                throw new InternalServerError();
            }

            if (daoList.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(daoList.get(0));
        });
    }

    @Override
    public void insertThreePidSession(IThreePidSessionDao session) {
        withCatcher(() -> {
            int updated = sessionDao.create(new ThreePidSessionDao(session));
            if (updated != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + updated);
            }
        });
    }

    @Override
    public void updateThreePidSession(IThreePidSessionDao session) {
        withCatcher(() -> {
            int updated = sessionDao.update(new ThreePidSessionDao(session));
            if (updated != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + updated);
            }
        });
    }

}
