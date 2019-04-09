/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
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

package io.kamax.mxisd;

import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.config.YamlConfigLoader;
import io.kamax.mxisd.exception.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public class MxisdStandaloneExec {

    private static final Logger log = LoggerFactory.getLogger("App");

    public static void main(String[] args) {
        try {
            MxisdConfig cfg = null;
            Iterator<String> argsIt = Arrays.asList(args).iterator();
            while (argsIt.hasNext()) {
                String arg = argsIt.next();
                if (StringUtils.equalsAny(arg, "-h", "--help", "-?", "--usage")) {
                    System.out.println("Available arguments:" + System.lineSeparator());
                    System.out.println("  -h, --help       Show this help message");
                    System.out.println("  --version        Print the version then exit");
                    System.out.println("  -c, --config     Set the configuration file location");
                    System.out.println(" ");
                    System.exit(0);
                } else if (StringUtils.equalsAny(arg, "-c", "--config")) {
                    String cfgFile = argsIt.next();
                    cfg = YamlConfigLoader.loadFromFile(cfgFile);
                } else if (StringUtils.equals("--version", arg)) {
                    System.out.println(Mxisd.Version);
                    System.exit(0);
                } else {
                    System.err.println("Invalid argument: " + arg);
                    System.err.println("Try '--help' for available arguments");
                    System.exit(1);
                }
            }

            log.info("mxisd starting");
            log.info("Version: {}", Mxisd.Version);

            if (Objects.isNull(cfg)) {
                cfg = YamlConfigLoader.tryLoadFromFile("mxisd.yaml").orElseGet(MxisdConfig::new);
            }

            HttpMxisd mxisd = new HttpMxisd(cfg);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                mxisd.stop();
                log.info("mxisd stopped");
            }));
            mxisd.start();

            log.info("mxisd started");
        } catch (ConfigurationException e) {
            log.error(e.getDetailedMessage());
            log.error(e.getMessage());
            System.exit(2);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

}
