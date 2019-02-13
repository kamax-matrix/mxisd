/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sàrl
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

package io.kamax.mxisd.config;

import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

public class YamlConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(YamlConfigLoader.class);

    public static MxisdConfig loadFromFile(String path) throws IOException {
        File f = new File(path).getAbsoluteFile();
        log.info("Reading config from {}", f.toString());
        Representer rep = new Representer();
        rep.getPropertyUtils().setBeanAccess(BeanAccess.FIELD);
        rep.getPropertyUtils().setAllowReadOnlyProperties(true);
        rep.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(new Constructor(MxisdConfig.class), rep);
        try (FileInputStream is = new FileInputStream(f)) {
            MxisdConfig raw = yaml.load(is);
            log.debug("Read config in memory from {}", path);

            // SnakeYaml set objects to null when there is no value set in the config, even a full sub-tree.
            // This is problematic for default config values and objects, to avoid NPEs.
            // Therefore, we'll use Gson to re-parse the data in a way that avoids us checking the whole config for nulls.
            MxisdConfig cfg = GsonUtil.get().fromJson(GsonUtil.get().toJson(raw), MxisdConfig.class);

            log.info("Loaded config from {}", path);
            return cfg;
        } catch (ParserException t) {
            throw new ConfigurationException(t.getMessage(), "Could not parse YAML config file - Please check indentation and that the configuration options exist");
        }
    }

    public static Optional<MxisdConfig> tryLoadFromFile(String path) {
        log.debug("Attempting to read config from {}", path);
        try {
            return Optional.of(loadFromFile(path));
        } catch (FileNotFoundException e) {
            log.info("No config file at {}", path);
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
