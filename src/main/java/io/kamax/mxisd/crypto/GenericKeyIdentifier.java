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

package io.kamax.mxisd.crypto;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class GenericKeyIdentifier implements KeyIdentifier {

    private final KeyType type;
    private final String algo;
    private final String serial;

    public GenericKeyIdentifier(KeyIdentifier id) {
        this(id.getType(), id.getAlgorithm(), id.getSerial());
    }

    public GenericKeyIdentifier(KeyType type, String algo, String serial) {
        if (StringUtils.isAnyBlank(algo, serial)) {
            throw new IllegalArgumentException("Algorithm and/or Serial cannot be blank");
        }

        this.type = Objects.requireNonNull(type);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GenericKeyIdentifier)) return false;
        GenericKeyIdentifier that = (GenericKeyIdentifier) o;
        return type == that.type &&
                algo.equals(that.algo) &&
                serial.equals(that.serial);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, algo, serial);
    }
}
