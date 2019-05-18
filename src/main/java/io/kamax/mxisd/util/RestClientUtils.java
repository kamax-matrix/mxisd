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

import com.google.gson.Gson;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class RestClientUtils {

    private static Gson gson = GsonUtil.build();

    public static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpPost post(String url, String body) {
        StringEntity entity = new StringEntity(body, StandardCharsets.UTF_8);
        entity.setContentType(ContentType.APPLICATION_JSON.toString());
        HttpPost req = new HttpPost(url);
        req.setEntity(entity);
        return req;
    }

    public static HttpPost post(String url, Gson gson, String member, Object o) {
        return post(url, JsonUtils.getObjAsString(gson, member, o));
    }

    public static HttpPost post(String url, Gson gson, Object o) {
        return post(url, gson.toJson(o));
    }

    public static HttpPost post(String url, Object o) {
        return post(url, gson.toJson(o));
    }

}
