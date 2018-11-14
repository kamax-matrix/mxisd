/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Kamax Sarl
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

package io.kamax.mxisd.lookup;

import io.kamax.matrix.ThreePid;

import java.time.Instant;

public class ThreePidValidation extends ThreePid {

    private Instant validation;

    public ThreePidValidation(ThreePid tpid, Instant validation) {
        super(tpid.getMedium(), tpid.getAddress());
        this.validation = validation;
    }

    public Instant getValidation() {
        return validation;
    }

}
