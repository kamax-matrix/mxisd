/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
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

package io.kamax.mxisd.exception;

import org.apache.http.HttpStatus;

import java.time.Instant;

public class InternalServerError extends MatrixException {

    private String reference = Long.toString(Instant.now().toEpochMilli());
    private String internalReason;

    public InternalServerError() {
        super(
                HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "M_UNKNOWN",
                "An internal server error occured. If this error persists, please contact support with reference #" +
                        Instant.now().toEpochMilli()
        );
    }

    public InternalServerError(String internalReason) {
        this();
        this.internalReason = internalReason;
    }

    public InternalServerError(Throwable t) {
        this(t.getMessage());
    }

    public String getReference() {
        return reference;
    }

    public String getInternalReason() {
        return internalReason;
    }

}
