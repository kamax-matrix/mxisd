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

import com.google.gson.JsonObject;
import io.kamax.matrix.crypto.KeyFileStore;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.exception.ObjectNotFoundException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FileKeyStore implements KeyStore {

    private static final Logger log = LoggerFactory.getLogger(FileKeyStore.class);

    private final String currentFilename = "current";
    private final String base;

    public FileKeyStore(String path) {
        base = new File(path).getAbsoluteFile().toString();
        File f = new File(base);

        if (!f.exists()) {
            if (!f.mkdir()) {
                throw new RuntimeException("Unable to create key store at " + f.toString());
            }
        } else {
            if (!f.isFile()) {
                log.debug("Key store is already in directory format");
            } else {
                try {
                    log.info("Found old key store format, migrating...");
                    File oldStorePath = new File(f.toString() + ".backup-before-migration");
                    FileUtils.moveFile(f, oldStorePath);
                    FileUtils.forceMkdir(f);


                    String privKey = new KeyFileStore(oldStorePath.toString()).load().orElse("");
                    if (StringUtils.isBlank(privKey)) {
                        throw new IllegalStateException("Signing key file is empty. Either fix or delete");
                    } else {
                        // We ensure this is valid Base64 data before migrating
                        Base64.decodeBase64(privKey);

                        // We store the new key
                        add(new GenericKey(new GenericKeyIdentifier(KeyType.Regular, KeyAlgorithm.Ed25519, "0"), true, privKey));

                        log.info("Store migrated to new directory format");
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Unable to migrate store from old single file format to new directory format", e);
                }
            }
        }

        if (!f.isDirectory()) {
            throw new RuntimeException("Key store path is not a directory: " + f.toString());
        }
    }

    private String toDirName(KeyType type) {
        return type.name().toLowerCase();
    }

    private Path ensureDirExists(KeyIdentifier id) {
        File b = Paths.get(base, toDirName(id.getType()), id.getAlgorithm()).toFile();

        if (b.exists()) {
            if (!b.isDirectory()) {
                throw new RuntimeException("Key store path already exists but is not a directory: " + b.toString());
            }
        } else {
            try {
                FileUtils.forceMkdir(b);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create key store path at " + b.toString(), e);
            }
        }

        return b.toPath();
    }

    @Override
    public boolean has(KeyIdentifier id) {
        return Paths.get(base, toDirName(id.getType()), id.getAlgorithm(), id.getSerial()).toFile().isFile();
    }

    @Override
    public List<KeyIdentifier> list() {
        List<KeyIdentifier> keyIds = new ArrayList<>();

        for (KeyType type : KeyType.values()) {
            keyIds.addAll(list(type));
        }

        return keyIds;
    }

    @Override
    public List<KeyIdentifier> list(KeyType type) {
        List<KeyIdentifier> keyIds = new ArrayList<>();

        File algoDir = Paths.get(base, toDirName(type)).toFile();
        File[] algos = algoDir.listFiles();
        if (Objects.isNull(algos)) {
            throw new IllegalStateException("Cannot list stored key algorithms: was expecting " + algoDir.toString() + " to be a directory");
        }

        for (File algo : algos) {
            File[] serials = algo.listFiles();
            if (Objects.isNull(serials)) {
                throw new IllegalStateException("Cannot list stored key serials: was expecting " + algo.toString() + " to be a directory");
            }

            for (File serial : serials) {
                keyIds.add(new GenericKeyIdentifier(type, algo.getName(), serial.getName()));
            }
        }

        return keyIds;
    }

    @Override
    public Key get(KeyIdentifier id) throws ObjectNotFoundException {
        File keyFile = ensureDirExists(id).resolve(id.getSerial()).toFile();
        if (!keyFile.exists() || !keyFile.isFile()) {
            throw new ObjectNotFoundException("Key", id.getId());
        }

        try (FileInputStream keyIs = new FileInputStream(keyFile)) {
            FileKeyJson json = GsonUtil.get().fromJson(IOUtils.toString(keyIs, StandardCharsets.UTF_8), FileKeyJson.class);
            return new GenericKey(id, json.isValid(), json.getKey());
        } catch (IOException e) {
            throw new RuntimeException("Unable to read key " + id.getId(), e);
        }
    }

    @Override
    public void add(Key key) throws IllegalStateException {
        File keyFile = ensureDirExists(key.getId()).resolve(key.getId().getSerial()).toFile();
        if (keyFile.exists()) {
            throw new IllegalStateException("Key " + key.getId().getId() + " already exists");
        }

        FileKeyJson json = FileKeyJson.get(key);
        try (FileOutputStream keyOs = new FileOutputStream(keyFile, false)) {
            IOUtils.write(GsonUtil.get().toJson(json), keyOs, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create key " + key.getId().getId(), e);
        }
    }

    @Override
    public void update(Key key) throws ObjectNotFoundException {
        File keyFile = ensureDirExists(key.getId()).resolve(key.getId().getSerial()).toFile();
        if (!keyFile.exists() || !keyFile.isFile()) {
            throw new ObjectNotFoundException("Key", key.getId().getId());
        }

        FileKeyJson json = FileKeyJson.get(key);
        try (FileOutputStream keyOs = new FileOutputStream(keyFile, false)) {
            IOUtils.write(GsonUtil.get().toJson(json), keyOs, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create key " + key.getId().getId(), e);
        }
    }

    @Override
    public void delete(KeyIdentifier id) throws ObjectNotFoundException {
        File keyFile = ensureDirExists(id).resolve(id.getSerial()).toFile();
        if (!keyFile.exists() || !keyFile.isFile()) {
            throw new ObjectNotFoundException("Key", id.getId());
        }

        if (!keyFile.delete()) {
            throw new RuntimeException("Unable to delete key " + id.getId());
        }
    }

    @Override
    public void setCurrentKey(KeyIdentifier id) throws IllegalArgumentException {
        JsonObject json = new JsonObject();
        json.addProperty("type", id.getType().name());
        json.addProperty("algo", id.getAlgorithm());
        json.addProperty("serial", id.getSerial());

        File f = Paths.get(base, currentFilename).toFile();

        try (FileOutputStream keyOs = new FileOutputStream(f, false)) {
            IOUtils.write(GsonUtil.get().toJson(json), keyOs, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write to " + f.toString(), e);
        }
    }

    @Override
    public Optional<KeyIdentifier> getCurrentKey() {
        File f = Paths.get(base, currentFilename).toFile();
        if (!f.exists()) {
            return Optional.empty();
        }

        if (!f.isFile()) {
            throw new IllegalStateException("Current key file is not a file: " + f.toString());
        }

        try (FileInputStream keyIs = new FileInputStream(f)) {
            JsonObject json = GsonUtil.parseObj(IOUtils.toString(keyIs, StandardCharsets.UTF_8));
            return Optional.of(new GenericKeyIdentifier(KeyType.valueOf(GsonUtil.getStringOrThrow(json, "type")), GsonUtil.getStringOrThrow(json, "algo"), GsonUtil.getStringOrThrow(json, "serial")));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read " + f.toString(), e);
        }
    }

}
