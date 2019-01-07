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
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public class MxisdStandaloneExec {

    public static void main(String[] args) throws IOException {
        MxisdConfig cfg = null;

        Iterator<String> argsIt = Arrays.asList(args).iterator();
        while (argsIt.hasNext()) {
            String arg = argsIt.next();
            if (StringUtils.equals("-c", arg)) {
                String cfgFile = argsIt.next();
                cfg = YamlConfigLoader.loadFromFile(cfgFile);
                System.out.println("Loaded configuration from " + cfgFile);
            } else {
                System.out.println("Invalid argument: " + arg);
                System.exit(1);
            }
        }

        if (Objects.isNull(cfg)) {
            cfg = YamlConfigLoader.tryLoadFromFile("mxisd.yaml").orElseGet(MxisdConfig::new);
        }

        try {
            HttpMxisd mxisd = new HttpMxisd(cfg);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                mxisd.stop();
                System.out.println("------------- mxisd stopped -------------");
            }));
            mxisd.start();

            System.out.println("------------- mxisd started -------------");
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

}
