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

package io.kamax.mxisd.spring;

import io.kamax.matrix.crypto.KeyFileStore;
import io.kamax.matrix.crypto.KeyManager;
import io.kamax.matrix.crypto.SignatureManager;
import io.kamax.mxisd.config.KeyConfig;
import io.kamax.mxisd.config.ServerConfig;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class CryptoFactory {

    public static KeyManager getKeyManager(KeyConfig keyCfg) {
        File keyStore = new File(keyCfg.getPath());
        if (!keyStore.exists()) {
            try {
                FileUtils.touch(keyStore);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new KeyManager(new KeyFileStore(keyCfg.getPath()));
    }

    public static SignatureManager getSignatureManager(KeyManager keyMgr, ServerConfig cfg) {
        return new SignatureManager(keyMgr, cfg.getName());
    }

}
