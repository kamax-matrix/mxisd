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

package io.kamax.mxisd.test.storage.crypto;

import io.kamax.mxisd.crypto.*;
import io.kamax.mxisd.exception.ObjectNotFoundException;
import io.kamax.mxisd.storage.crypto.KeyStore;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public abstract class KeyStoreTest {

    private KeyStore store;

    public abstract KeyStore create() throws Exception;

    private Key generateRandomKey() {
        KeyIdentifier keyId = new GenericKeyIdentifier(KeyType.Regular, "algo", RandomStringUtils.randomAlphanumeric(6));
        return new GenericKey(keyId, true, RandomStringUtils.randomAlphanumeric(48));
    }

    @Before
    public void before() throws Exception {
        store = create();
    }

    @Test
    public void isEmptyAfterCreate() {
        assertTrue(store.list().isEmpty());
        assertFalse(store.getCurrentKey().isPresent());
    }

    @Test
    public void add() {
        Key key = generateRandomKey();
        KeyIdentifier keyId = key.getId();

        store.add(key);

        Key keyFromStore = store.get(keyId);
        assertEquals(key.getId(), keyFromStore.getId());
        assertEquals(key.getPrivateKeyBase64(), keyFromStore.getPrivateKeyBase64());
        assertEquals(key.isValid(), keyFromStore.isValid());

        assertTrue(store.list().contains(keyId));
        assertTrue(store.list(keyId.getType()).contains(keyId));
    }

    @Test(expected = IllegalStateException.class)
    public void addDuplicate() {
        Key key = generateRandomKey();
        store.add(key);
        store.add(key);
    }

    @Test
    public void update() {
        Key key = generateRandomKey();
        store.add(key);

        Key keyUpdated = new GenericKey(key.getId(), !key.isValid(), key.getPrivateKeyBase64());
        store.update(keyUpdated);

        Key keyFromStore = store.get(key.getId());
        assertEquals(key.getId(), keyFromStore.getId());
        assertEquals(key.getPrivateKeyBase64(), keyFromStore.getPrivateKeyBase64());
        assertEquals(key.isValid(), !keyFromStore.isValid());
    }

    @Test(expected = ObjectNotFoundException.class)
    public void updateNonExisting() {
        store.update(generateRandomKey());
    }

    @Test
    public void delete() {
        Key key = generateRandomKey();
        store.add(key);

        store.delete(key.getId());
        assertFalse(store.list().contains(key.getId()));
        assertFalse(store.list(key.getId().getType()).contains(key.getId()));
    }

    @Test(expected = ObjectNotFoundException.class)
    public void deleteNonExisting() {
        store.delete(generateRandomKey().getId());
    }

    @Test
    public void setCurrentKey() {
        Key key = generateRandomKey();
        store.add(key);
        store.setCurrentKey(key.getId());
        Optional<KeyIdentifier> currentKey = store.getCurrentKey();
        assertTrue(currentKey.isPresent());
        assertEquals(currentKey.get(), key.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setCurrentKeyNonExisting() {
        store.setCurrentKey(generateRandomKey().getId());
    }

}
