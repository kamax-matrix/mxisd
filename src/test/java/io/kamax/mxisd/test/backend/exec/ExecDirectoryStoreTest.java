/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
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

package io.kamax.mxisd.test.backend.exec;

import io.kamax.matrix.MatrixID;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.backend.exec.ExecDirectoryStore;
import io.kamax.mxisd.backend.exec.ExecStore;
import io.kamax.mxisd.config.ExecConfig;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.http.io.UserDirectorySearchResult;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Collections;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecDirectoryStoreTest extends ExecStoreTest {

    public ExecDirectoryStoreTest() {
        executables.put("byNameSuccessEmptyResult", () ->
                make(0, () ->
                        GsonUtil.get().toJson(UserDirectorySearchResult.empty())
                )
        );

        executables.put("byNameSuccessSingleResult", () -> make(0, () -> {
            UserDirectorySearchResult.Result resultIo = new UserDirectorySearchResult.Result();
            resultIo.setUserId(user1Localpart);
            resultIo.setDisplayName(user1Name);
            UserDirectorySearchResult io = new UserDirectorySearchResult();
            io.setLimited(false);
            io.setResults(Collections.singleton(resultIo));
            return GsonUtil.get().toJson(io);
        }));
    }

    private ExecConfig getCfg() {
        ExecConfig cfg = new ExecConfig().build();
        assertFalse(cfg.isEnabled());
        cfg.setEnabled(true);
        assertTrue(cfg.isEnabled());
        cfg.getDirectory().getSearch().getByName().getOutput().setType(ExecStore.JsonType);
        return cfg;
    }

    private ExecDirectoryStore getStore(ExecConfig cfg) {
        ExecDirectoryStore store = new ExecDirectoryStore(cfg, getMatrixCfg());
        store.setExecutorSupplier(this::build);
        return store;
    }

    private ExecDirectoryStore getStore(String command) {
        ExecConfig cfg = getCfg();
        cfg.getDirectory().getSearch().getByName().setCommand(command);
        cfg.getDirectory().getSearch().getByThreepid().setCommand(command);
        return getStore(cfg);
    }

    @Test
    public void byNameNoCommandDefined() {
        ExecConfig cfg = getCfg();
        assertTrue(StringUtils.isEmpty(cfg.getDirectory().getSearch().getByName().getCommand()));
        ExecDirectoryStore store = getStore(cfg);

        UserDirectorySearchResult result = store.searchByDisplayName("user");
        assertFalse(result.isLimited());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    public void byNameSuccessNoOutput() {
        UserDirectorySearchResult result = getStore(sno).searchByDisplayName("user");
        assertFalse(result.isLimited());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    public void byNameSuccessEmptyResult() {
        UserDirectorySearchResult output = getStore("byNameSuccessEmptyResult").searchByDisplayName("user");
        assertFalse(output.isLimited());
        assertTrue(output.getResults().isEmpty());
    }

    @Test
    public void byNameSuccessSingleResult() {
        UserDirectorySearchResult output = getStore("byNameSuccessSingleResult").searchByDisplayName("user");

        assertFalse(output.isLimited());
        assertEquals(1, output.getResults().size());
        UserDirectorySearchResult.Result result = output.getResults().iterator().next();
        assertEquals(MatrixID.from(user1Localpart, domain).acceptable().getId(), result.getUserId());
        assertEquals(user1Name, result.getDisplayName());
    }

    @Test
    public void byNameFailureNoOutput() {
        UserDirectorySearchResult result = getStore(fno).searchByDisplayName("user");
        assertFalse(result.isLimited());
        assertTrue(result.getResults().isEmpty());
    }

    @Test(expected = InternalServerError.class)
    public void byNameUnknownNoOutput() {
        getStore(uno).searchByDisplayName("user");
    }

}
