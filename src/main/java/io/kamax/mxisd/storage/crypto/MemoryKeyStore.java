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

import io.kamax.mxisd.crypto.*;
import io.kamax.mxisd.exception.ObjectNotFoundException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryKeyStore implements KeyStore {

    private Map<KeyType, Map<String, Map<String, FileKeyJson>>> keys = new ConcurrentHashMap<>();
    private KeyIdentifier current;

    private Map<String, FileKeyJson> getMap(KeyType type, String algo) {
        return keys.computeIfAbsent(type, k -> new ConcurrentHashMap<>()).computeIfAbsent(algo, k -> new ConcurrentHashMap<>());
    }

    @Override
    public boolean has(KeyIdentifier id) {
        return getMap(id.getType(), id.getAlgorithm()).containsKey(id.getSerial());
    }

    @Override
    public List<KeyIdentifier> list() {
        List<KeyIdentifier> keyIds = new ArrayList<>();
        keys.forEach((key, value) -> value.forEach((key1, value1) -> value1.forEach((key2, value2) -> keyIds.add(new GenericKeyIdentifier(key, key1, key2)))));
        return keyIds;
    }

    @Override
    public List<KeyIdentifier> list(KeyType type) {
        List<KeyIdentifier> keyIds = new ArrayList<>();
        keys.computeIfAbsent(type, t -> new ConcurrentHashMap<>()).forEach((key, value) -> value.forEach((key1, value1) -> keyIds.add(new GenericKeyIdentifier(type, key, key1))));
        return keyIds;
    }

    @Override
    public Key get(KeyIdentifier id) throws ObjectNotFoundException {
        FileKeyJson data = getMap(id.getType(), id.getAlgorithm()).get(id.getSerial());
        if (Objects.isNull(data)) {
            throw new ObjectNotFoundException("Key", id.getType() + ":" + id.getAlgorithm() + ":" + id.getSerial());
        }

        return new GenericKey(new GenericKeyIdentifier(id), data.isValid(), data.getKey());
    }

    private void set(Key key) {
        FileKeyJson data = FileKeyJson.get(key);
        getMap(key.getId().getType(), key.getId().getAlgorithm()).put(key.getId().getSerial(), data);
    }

    @Override
    public void add(Key key) throws IllegalStateException {
        if (has(key.getId())) {
            throw new IllegalStateException("Key " + key.getId().getId() + " already exists");
        }

        set(key);
    }

    @Override
    public void update(Key key) throws ObjectNotFoundException {
        if (!has(key.getId())) {
            throw new ObjectNotFoundException("Key", key.getId().getType() + ":" + key.getId().getAlgorithm() + ":" + key.getId().getSerial());
        }

        set(key);
    }

    @Override
    public void delete(KeyIdentifier id) throws ObjectNotFoundException {
        if (!has(id)) {
            throw new ObjectNotFoundException("Key", id.getType() + ":" + id.getAlgorithm() + ":" + id.getSerial());
        }

        keys.computeIfAbsent(id.getType(), k -> new ConcurrentHashMap<>()).computeIfAbsent(id.getAlgorithm(), k -> new ConcurrentHashMap<>()).remove(id.getSerial());
    }

    @Override
    public void setCurrentKey(KeyIdentifier id) throws IllegalArgumentException {
        if (!has(id)) {
            throw new IllegalArgumentException("Key " + id.getType() + ":" + id.getAlgorithm() + ":" + id.getSerial() + " is not known to the store");
        }

        current = id;
    }

    @Override
    public Optional<KeyIdentifier> getCurrentKey() {
        return Optional.ofNullable(current);
    }

}
