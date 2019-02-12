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

package io.kamax.mxisd.crypto;

import io.kamax.mxisd.config.KeyConfig;
import io.kamax.mxisd.storage.crypto.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

public class CryptoFactory {

    public static Ed25519KeyManager getKeyManager(KeyConfig keyCfg) {
        KeyStore store;
        if (StringUtils.equals(":memory:", keyCfg.getPath())) {
            store = new MemoryKeyStore();
        } else {
            File keyStore = new File(keyCfg.getPath());
            if (!keyStore.exists()) {
                try {
                    FileUtils.touch(keyStore);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            store = new FileKeyStore(keyCfg.getPath());
        }

        return new Ed25519KeyManager(store);
    }

    public static SignatureManager getSignatureManager(Ed25519KeyManager keyMgr) {
        return new Ed25519SignatureManager(keyMgr);
    }

}
