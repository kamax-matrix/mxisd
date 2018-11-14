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

package io.kamax.mxisd.exception;

import org.apache.http.HttpStatus;

public class FeatureNotAvailable extends HttpMatrixException {

    private String internalReason;

    public FeatureNotAvailable(String internalReason) {
        super(
                HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "M_NOT_AVAILABLE",
                "This action is currently not available. Contact your administrator to enable it."
        );

        this.internalReason = internalReason;
    }

    public String getInternalReason() {
        return internalReason;
    }

}
