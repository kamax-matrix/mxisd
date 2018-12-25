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
import io.kamax.mxisd.exception.InvalidResponseJsonException;
import io.kamax.mxisd.exception.JsonMemberNotFoundException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class GsonParser {

    private JsonParser parser = new JsonParser();
    private Gson gson;

    public GsonParser() {
        this(new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create());
    }

    public GsonParser(Gson gson) {
        this.gson = gson;
    }

    public JsonObject parse(InputStream stream) throws IOException {
        JsonElement el = parser.parse(IOUtils.toString(stream, StandardCharsets.UTF_8));
        if (!el.isJsonObject()) {
            throw new InvalidResponseJsonException("Response body is not a JSON object");
        }

        return el.getAsJsonObject();
    }

    public <T> T parse(HttpResponse res, Class<T> type) throws IOException {
        return gson.fromJson(parse(res.getEntity().getContent()), type);
    }

    public Optional<JsonObject> parseOptional(HttpResponse res) {
        try {
            return Optional.of(parse(res.getEntity().getContent()));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public JsonObject parse(InputStream stream, String property) throws IOException {
        JsonObject obj = parse(stream);
        if (!obj.has(property)) {
            throw new JsonMemberNotFoundException("Member " + property + " does not exist");
        }

        JsonElement el = obj.get(property);
        if (!el.isJsonObject()) {
            throw new InvalidResponseJsonException("Member " + property + " is not a JSON object");
        }

        return el.getAsJsonObject();
    }

    public <T> T parse(InputStream stream, String memberName, Class<T> type) throws IOException {
        JsonObject obj = parse(stream, memberName);
        return gson.fromJson(obj, type);
    }

    public <T> T parse(HttpResponse res, String memberName, Class<T> type) throws IOException {
        return parse(res.getEntity().getContent(), memberName, type);
    }

    public <T> Optional<T> parseOptional(HttpResponse res, String memberName, Class<T> type) throws IOException {
        try {
            return Optional.of(parse(res.getEntity().getContent(), memberName, type));
        } catch (JsonMemberNotFoundException e) {
            return Optional.empty();
        }
    }

}
