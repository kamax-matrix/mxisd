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

import io.kamax.mxisd.exception.ObjectNotFoundException;

import java.util.List;
import java.util.Optional;

/**
 * Store to persist signing keys and the identifier for the current long-term signing key
 */
public interface KeyStore {

    /**
     * If a given key is currently stored
     *
     * @param id The Identifier elements for the key
     * @return true if the key is stored, false if not
     */
    boolean has(KeyIdentifier id);

    /**
     * List all keys within the store
     *
     * @return The list of key identifiers
     */
    List<KeyIdentifier> list();

    /**
     * List all keys of a given type within the store
     *
     * @param type The type to filter on
     * @return The list of keys identifiers matching the given type
     */
    List<KeyIdentifier> list(KeyType type);

    /**
     * Get the key that relates to the given identifier
     *
     * @param id The identifier of the key to get
     * @return The key
     * @throws ObjectNotFoundException If no key is found for that identifier
     */
    Key get(KeyIdentifier id) throws ObjectNotFoundException;

    /**
     * Add a key to the store
     *
     * @param key The key to store
     * @throws IllegalStateException If a key already exist for the given identifier data
     */
    void add(Key key) throws IllegalStateException;

    void update(Key key) throws ObjectNotFoundException;

    /**
     * Delete a key from the store
     *
     * @param id The key identifier of the key to delete
     * @throws ObjectNotFoundException If no key is found for that identifier
     */
    void delete(KeyIdentifier id) throws ObjectNotFoundException;

    /**
     * Store the information of which key is the current signing key
     *
     * @param id The key identifier
     * @throws ObjectNotFoundException If the key is not known to the store
     */
    void setCurrentKey(KeyIdentifier id) throws ObjectNotFoundException;

    /**
     * Retrieve the previously stored information of which key is the current signing key, if any
     *
     * @return The optional key identifier that was previously stored
     */
    Optional<KeyIdentifier> getCurrentKey();

}
