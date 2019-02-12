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

public class GenericKeyIdentifier implements KeyIdentifier {

    private final KeyType type;
    private final String algo;
    private final String serial;

    public GenericKeyIdentifier(KeyIdentifier id) {
        this(id.getType(), id.getAlgorithm(), id.getSerial());
    }

    public GenericKeyIdentifier(KeyType type, String algo, String serial) {
        this.type = type;
        this.algo = algo;
        this.serial = serial;
    }

    @Override
    public KeyType getType() {
        return type;
    }

    @Override
    public String getAlgorithm() {
        return algo;
    }

    @Override
    public String getSerial() {
        return serial;
    }

}
