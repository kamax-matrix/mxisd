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
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import io.kamax.mxisd.storage.IStorage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OrmLiteSqliteStorage implements IStorage {

    private Dao<ThreePidInviteIO, String> invDao;

    OrmLiteSqliteStorage(String path) {
        try {
            File parent = new File(path).getParentFile();
            if (!parent.mkdirs() && !parent.isDirectory()) {
                throw new RuntimeException("Unable to create DB parent directory: " + parent);
            }

            ConnectionSource connPool = new JdbcConnectionSource("jdbc:sqlite:" + path);
            invDao = DaoManager.createDao(connPool, ThreePidInviteIO.class);
            TableUtils.createTableIfNotExists(connPool, ThreePidInviteIO.class);
        } catch (SQLException e) {
            throw new RuntimeException(e); // FIXME do better
        }
    }

    @Override
    public Collection<ThreePidInviteIO> getInvites() {
        try (CloseableWrappedIterable<ThreePidInviteIO> t = invDao.getWrappedIterable()) {
            List<ThreePidInviteIO> ioList = new ArrayList<>();
            t.forEach(ioList::add);
            return ioList;
        } catch (IOException e) {
            throw new RuntimeException(e); // FIXME do better
        }
    }

    @Override
    public void insertInvite(IThreePidInviteReply data) {
        try {
            int updated = invDao.create(new ThreePidInviteIO(data));
            if (updated != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + updated);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e); // FIXME do better
        }
    }

    @Override
    public void deleteInvite(String id) {
        try {
            int updated = invDao.deleteById(id);
            if (updated != 1) {
                throw new RuntimeException("Unexpected row count after DB action: " + updated);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e); // FIXME do better
        }
    }

}
