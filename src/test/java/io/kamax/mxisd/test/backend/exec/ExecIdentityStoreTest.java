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

import com.google.gson.JsonObject;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.UserID;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.backend.exec.ExecIdentityStore;
import io.kamax.mxisd.backend.exec.ExecStore;
import io.kamax.mxisd.backend.rest.LookupSingleResponseJson;
import io.kamax.mxisd.config.ExecConfig;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import org.junit.Test;

import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecIdentityStoreTest extends ExecStoreTest {

    public ExecIdentityStoreTest() {
        executables.put("singleSuccessEmpty", () -> make(0, () ->
                GsonUtil.get().toJson(GsonUtil.makeObj("lookup", new JsonObject()))));

        executables.put("singleSuccessData", () -> makeJson(0, () -> {
            LookupSingleResponseJson json = new LookupSingleResponseJson();
            json.setMedium(ThreePidMedium.Email);
            json.setAddress(user1Email);
            json.setId(new UserID(UserIdType.Localpart, user1Localpart));
            return GsonUtil.makeObj("lookup", GsonUtil.get().toJsonTree(json));
        }));

        executables.put("singleSuccessEmptyFromInvalidOutput", () -> makeJson(0, () -> {
            JsonObject lookup = new JsonObject();
            lookup.addProperty("medium", "");
            return GsonUtil.makeObj("lookup", lookup);
        }));
    }

    private ExecConfig getCfg() {
        ExecConfig cfg = new ExecConfig().build();
        assertFalse(cfg.isEnabled());
        cfg.setEnabled(true);
        assertTrue(cfg.isEnabled());
        cfg.getIdentity().getLookup().getSingle().getOutput().setType(ExecStore.JsonType);
        cfg.getIdentity().getLookup().getBulk().getOutput().setType(ExecStore.JsonType);
        return cfg;
    }

    private ExecIdentityStore getStore(ExecConfig cfg) {
        ExecIdentityStore store = new ExecIdentityStore(cfg, getMatrixCfg());
        store.setExecutorSupplier(this::build);
        assertTrue(store.isLocal());
        return store;
    }

    private ExecIdentityStore getStore(String command) {
        ExecConfig cfg = getCfg();
        cfg.getIdentity().getLookup().getSingle().setCommand(command);
        cfg.getIdentity().getLookup().getBulk().setCommand(command);
        return getStore(cfg);
    }

    @Test
    public void singleSuccessNoOutput() {
        ExecIdentityStore store = getStore(sno);

        SingleLookupRequest req = new SingleLookupRequest();
        req.setType(ThreePidMedium.Email.getId());
        req.setThreePid(user1Email);
        Optional<SingleLookupReply> lookup = store.find(req);
        assertFalse(lookup.isPresent());
    }

    @Test
    public void singleSuccessEmpty() {
        ExecIdentityStore store = getStore("singleSuccessEmpty");

        SingleLookupRequest req = new SingleLookupRequest();
        req.setType(ThreePidMedium.Email.getId());
        req.setThreePid(user1Email);
        Optional<SingleLookupReply> lookup = store.find(req);
        assertFalse(lookup.isPresent());
    }

    @Test
    public void singleSuccessData() {
        SingleLookupRequest req = new SingleLookupRequest();
        req.setType(ThreePidMedium.Email.getId());
        req.setThreePid(user1Email);

        Optional<SingleLookupReply> lookup = getStore("singleSuccessData").find(req);
        assertTrue(lookup.isPresent());
        SingleLookupReply reply = lookup.get();
        assertEquals(MatrixID.asAcceptable(user1Localpart, domain), reply.getMxid());
    }

    @Test(expected = InternalServerError.class)
    public void singleSuccessEmptyFromInvalidOutput() {
        SingleLookupRequest req = new SingleLookupRequest();
        req.setType(ThreePidMedium.Email.getId());
        req.setThreePid(user1Email);
        getStore("singleSuccessEmptyFromInvalidOutput").find(req);
    }

}
