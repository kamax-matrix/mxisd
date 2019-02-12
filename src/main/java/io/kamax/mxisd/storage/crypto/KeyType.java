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

/**
 * Types of keys used by an Identity server.
 * See https://matrix.org/docs/spec/identity_service/r0.1.0.html#key-management
 */
public enum KeyType {

    /**
     * Ephemeral keys are related to 3PID invites and are only valid while the invite is pending.
     */
    Ephemeral,

    /**
     * Regular keys are used by the Identity Server itself to sign requests/responses
     */
    Regular

}
