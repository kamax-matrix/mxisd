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

package io.kamax.mxisd.util;

import com.google.gson.*;

import java.util.Optional;

public class GsonUtil {

    public static Gson build() {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    public static Optional<JsonElement> findElement(JsonObject o, String key) {
        return Optional.ofNullable(o.get(key));
    }

    public static Optional<JsonObject> findObj(JsonObject o, String key) {
        return findElement(o, key).map(el -> el.isJsonObject() ? el.getAsJsonObject() : null);
    }

    public static Optional<JsonPrimitive> findPrimitive(JsonObject o, String key) {
        return findElement(o, key).map(el -> el.isJsonPrimitive() ? el.getAsJsonPrimitive() : null);
    }

}
