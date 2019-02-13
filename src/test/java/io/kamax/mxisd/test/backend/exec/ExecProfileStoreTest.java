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
import io.kamax.matrix.ThreePid;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.matrix._ThreePid;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.backend.exec.ExecProfileStore;
import io.kamax.mxisd.backend.exec.ExecStore;
import io.kamax.mxisd.config.ExecConfig;
import io.kamax.mxisd.profile.JsonProfileResult;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecProfileStoreTest extends ExecStoreTest {

    private final String seo = "successEmptyOutput";
    private final String sn = "successName";
    private final String sst = "successSingleThreepid";
    private final String smt = "successMultiThreepid";
    private final String user1Msisdn = Long.toString(System.currentTimeMillis());

    public ExecProfileStoreTest() {
        executables.put(seo, () -> make(0, () -> "{}"));
        executables.put(sn, () -> makeJson(0, () -> {
            JsonObject profile = new JsonObject();
            profile.addProperty("display_name", user1Name);
            return GsonUtil.makeObj("profile", profile);
        }));
        executables.put(sst, () -> makeJson(0, () -> {
            JsonProfileResult profile = new JsonProfileResult();
            profile.addThreepid(new ThreePid(ThreePidMedium.Email.getId(), user1Email));
            return GsonUtil.makeObj("profile", profile);
        }));

        executables.put(smt, () -> makeJson(0, () -> {
            JsonProfileResult profile = new JsonProfileResult();
            profile.addThreepid(new ThreePid(ThreePidMedium.Email.getId(), user1Email));
            profile.addThreepid(new ThreePid(ThreePidMedium.PhoneNumber.getId(), user1Msisdn));
            return GsonUtil.makeObj("profile", profile);
        }));

    }

    private ExecConfig getCfg() {
        ExecConfig cfg = new ExecConfig().build();
        assertFalse(cfg.isEnabled());
        cfg.setEnabled(true);
        assertTrue(cfg.isEnabled());
        cfg.getProfile().getDisplayName().getOutput().setType(ExecStore.JsonType);
        cfg.getProfile().getThreePid().getOutput().setType(ExecStore.JsonType);
        cfg.getProfile().getRole().getOutput().setType(ExecStore.JsonType);
        return cfg;
    }

    private ExecProfileStore getStore(ExecConfig cfg) {
        ExecProfileStore store = new ExecProfileStore(cfg);
        store.setExecutorSupplier(this::build);
        return store;
    }

    private ExecProfileStore getStore(String command) {
        ExecConfig cfg = getCfg();
        cfg.getProfile().getDisplayName().setCommand(command);
        cfg.getProfile().getThreePid().setCommand(command);
        cfg.getProfile().getRole().setCommand(command);
        return getStore(cfg);
    }

    @Test
    public void getNameSuccessNoOutput() {
        Optional<String> name = getStore(sno).getDisplayName(MatrixID.asAcceptable(user1Localpart, domain));
        assertFalse(name.isPresent());
    }

    @Test
    public void getNameSuccessEmptyOutput() {
        Optional<String> name = getStore(seo).getDisplayName(MatrixID.asAcceptable(user1Localpart, domain));
        assertFalse(name.isPresent());
    }

    @Test
    public void getNameSuccess() {
        Optional<String> name = getStore(sn).getDisplayName(user1Id);
        assertTrue(name.isPresent());
        assertEquals(user1Name, name.get());
    }

    @Test
    public void getSingleThreePidSuccess() {
        List<_ThreePid> tpids = getStore(sst).getThreepids(user1Id);
        assertEquals(1, tpids.size());

        _ThreePid tpid = tpids.get(0);
        assertEquals(UserIdType.Email.getId(), tpid.getMedium());
        assertEquals(user1Email, tpid.getAddress());
    }

    @Test
    public void getMultiThreePidSuccess() {
        List<_ThreePid> tpids = getStore(smt).getThreepids(user1Id);
        assertEquals(2, tpids.size());

        _ThreePid firstTpid = tpids.get(0);
        assertEquals(ThreePidMedium.Email.getId(), firstTpid.getMedium());
        assertEquals(user1Email, firstTpid.getAddress());

        _ThreePid secondTpid = tpids.get(1);
        assertEquals(ThreePidMedium.PhoneNumber.getId(), secondTpid.getMedium());
        assertEquals(user1Msisdn, secondTpid.getAddress());
    }

}
