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

package io.kamax.mxisd.config.threepid.medium;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class MediumConfig {

    private String connector;
    private Map<String, JsonObject> connectors = new HashMap<>();
    private String generator;
    private Map<String, JsonObject> generators = new HashMap<>();

    public String getConnector() {
        return connector;
    }

    public void setConnector(String connector) {
        this.connector = connector;
    }

    public Map<String, JsonObject> getConnectors() {
        return connectors;
    }

    public void setConnectors(Map<String, JsonObject> connectors) {
        this.connectors = connectors;
    }

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public Map<String, JsonObject> getGenerators() {
        return generators;
    }

    public void setGenerators(Map<String, JsonObject> generators) {
        this.generators = generators;
    }

}
