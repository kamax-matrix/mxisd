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

public class GenericKey implements Key {

    private final KeyIdentifier id;
    private final boolean isValid;
    private final String privKey;

    public GenericKey(KeyIdentifier id, boolean isValid, String privKey) {
        this.id = new GenericKeyIdentifier(id);
        this.isValid = isValid;
        this.privKey = privKey;
    }


    @Override
    public KeyIdentifier getId() {
        return id;
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public String getPrivateKeyBase64() {
        return privKey;
    }

}
