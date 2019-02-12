/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2019 Kamax Sàrl
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

package io.kamax.mxisd.storage.crypto;

public class Ed25519Key implements Key {

    private KeyIdentifier id;
    private String privKey;

    public Ed25519Key(KeyIdentifier id, String privKey) {
        if (!KeyAlgorithm.Ed25519.equals(id.getAlgorithm())) {
            throw new IllegalArgumentException();
        }

        this.id = new GenericKeyIdentifier(id);
        this.privKey = privKey;
    }


    @Override
    public KeyIdentifier getId() {
        return id;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String getPrivateKeyBase64() {
        return privKey;
    }

}
