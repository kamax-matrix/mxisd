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
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.MatrixConfig;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExecStoreTest {

    protected final String sno = "successNoOutput";
    protected final String fno = "failureNoOutput";
    protected final String uno = "unknownNoOutput";

    protected final String domain = "domain.tld";
    protected final String userLocalpart = "user";
    protected final String user1Localpart = userLocalpart + "1";
    protected final String user1Name = "User 1";
    protected final String user2Localpart = userLocalpart + "2";
    protected final String user2Name = "User 2";
    protected final _MatrixID user1Id = MatrixID.asAcceptable(user1Localpart, domain);
    protected final String user1Email = user1Localpart + "@" + domain;

    protected Map<String, Supplier<ProcessResult>> executables = new HashMap<>();

    public ExecStoreTest() {
        executables.put(sno, () -> make(0, () -> ""));
        executables.put(fno, () -> make(1, () -> ""));
        executables.put(uno, () -> make(Integer.MAX_VALUE, () -> ""));
    }

    protected MatrixConfig getMatrixCfg() {
        MatrixConfig mxCfg = new MatrixConfig();
        mxCfg.setDomain(domain);
        return mxCfg;
    }

    protected ProcessResult make(int exitCode, Supplier<String> supplier) {
        return new ProcessResult(exitCode, null) {

            @Override
            public String outputUTF8() {
                return supplier.get();
            }

        };
    }

    protected ProcessResult makeJson(int exitCode, Supplier<Object> supplier) {
        return make(exitCode, () -> GsonUtil.get().toJson(supplier.get()));
    }

    protected ProcessExecutor build() {
        return new ProcessExecutor() {

            private Function<String, RuntimeException> notFound = command ->
                    new IllegalArgumentException("Command not found: " + command);

            @Override
            public ProcessResult execute() {
                if (getCommand().size() == 0) throw new IllegalStateException();

                String command = getCommand().get(0);
                return executables.getOrDefault(command, () -> {
                    throw notFound.apply(command);
                }).get();
            }

        };
    }

}
