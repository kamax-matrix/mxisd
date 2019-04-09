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

package io.kamax.mxisd.http.undertow.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.dns.ClientDnsOverwrite;
import io.kamax.mxisd.exception.AccessTokenNotFoundException;
import io.kamax.mxisd.exception.HttpMatrixException;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.proxy.Response;
import io.kamax.mxisd.util.RestClientUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

public abstract class BasicHttpHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(BasicHttpHandler.class);

    protected String getAccessToken(HttpServerExchange exchange) {
        return Optional.ofNullable(exchange.getRequestHeaders().getFirst("Authorization"))
                .flatMap(v -> {
                    if (!v.startsWith("Bearer ")) {
                        return Optional.empty();
                    }

                    return Optional.of(v.substring("Bearer ".length()));
                }).filter(StringUtils::isNotEmpty)
                .orElseThrow(AccessTokenNotFoundException::new);
    }

    protected String getRemoteHostAddress(HttpServerExchange exchange) {
        return ((InetSocketAddress) exchange.getConnection().getPeerAddress()).getAddress().getHostAddress();
    }

    protected String getQueryParameter(HttpServerExchange exchange, String name) {
        return getQueryParameter(exchange.getQueryParameters(), name);
    }

    protected String getQueryParameter(Map<String, Deque<String>> parms, String name) {
        try {
            String raw = parms.getOrDefault(name, new LinkedList<>()).peekFirst();
            if (StringUtils.isEmpty(raw)) {
                return raw;
            }

            return URLDecoder.decode(raw, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new InternalServerError(e);
        }
    }

    protected String getPathVariable(HttpServerExchange exchange, String name) {
        return getQueryParameter(exchange, name);
    }

    protected Optional<String> getContentType(HttpServerExchange exchange) {
        return Optional.ofNullable(exchange.getRequestHeaders().getFirst("Content-Type"));
    }

    protected void writeBodyAsUtf8(HttpServerExchange exchange, String body) {
        exchange.getResponseSender().send(body, StandardCharsets.UTF_8);
    }

    protected String getBodyUtf8(HttpServerExchange exchange) {
        try {
            return IOUtils.toString(exchange.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T parseJsonTo(HttpServerExchange exchange, Class<T> type) {
        return GsonUtil.get().fromJson(getBodyUtf8(exchange), type);
    }

    protected JsonObject parseJsonObject(HttpServerExchange exchange, String key) {
        return GsonUtil.getObj(parseJsonObject(exchange), key);
    }

    protected JsonObject parseJsonObject(HttpServerExchange exchange) {
        return GsonUtil.parseObj(getBodyUtf8(exchange));
    }

    protected void putHeader(HttpServerExchange ex, String name, String value) {
        ex.getResponseHeaders().put(HttpString.tryFromString(name), value);
    }

    protected void respond(HttpServerExchange ex, int statusCode, JsonElement bodyJson) {
        respondJson(ex, statusCode, GsonUtil.get().toJson(bodyJson));
    }

    protected void respond(HttpServerExchange ex, JsonElement bodyJson) {
        respond(ex, 200, bodyJson);
    }

    protected void respondJson(HttpServerExchange ex, int status, String body) {
        ex.setStatusCode(status);
        ex.getResponseHeaders().put(HttpString.tryFromString("Content-Type"), "application/json");
        writeBodyAsUtf8(ex, body);
    }

    protected void respondJson(HttpServerExchange ex, String body) {
        respondJson(ex, 200, body);
    }

    protected void respondJson(HttpServerExchange ex, Object body) {
        respondJson(ex, GsonUtil.get().toJson(body));
    }

    protected JsonObject buildErrorBody(HttpServerExchange exchange, String errCode, String error) {
        JsonObject obj = new JsonObject();
        obj.addProperty("errcode", errCode);
        obj.addProperty("error", error);
        obj.addProperty("success", false);
        log.info("Request {} {} - Error {}: {}", exchange.getRequestMethod(), exchange.getRequestURL(), errCode, error);
        return obj;
    }

    protected void respond(HttpServerExchange ex, int status, String errCode, String error) {
        respond(ex, status, buildErrorBody(ex, errCode, error));
    }

    protected void handleException(HttpServerExchange exchange, HttpMatrixException ex) {
        respond(exchange, ex.getStatus(), buildErrorBody(exchange, ex.getErrorCode(), ex.getError()));
    }

    protected void respond(HttpServerExchange exchange, Response upstream) {
        exchange.setStatusCode(upstream.getStatus());
        upstream.getHeaders().forEach((key, value) -> exchange.getResponseHeaders().addAll(HttpString.tryFromString(key), value));
        writeBodyAsUtf8(exchange, upstream.getBody());
    }

    protected void proxyPost(HttpServerExchange exchange, JsonObject body, CloseableHttpClient client, ClientDnsOverwrite dns) {
        String target = dns.transform(URI.create(exchange.getRequestURL())).toString();
        log.info("Requesting remote: {}", target);
        HttpPost req = RestClientUtils.post(target, GsonUtil.get(), body);

        exchange.getRequestHeaders().forEach(header -> {
            header.forEach(v -> {
                String name = header.getHeaderName().toString();
                if (!StringUtils.startsWithIgnoreCase(name, "content-")) {
                    req.addHeader(name, v);
                }
            });
        });

        try (CloseableHttpResponse res = client.execute(req)) {
            exchange.setStatusCode(res.getStatusLine().getStatusCode());
            for (Header h : res.getAllHeaders()) {
                for (HeaderElement el : h.getElements()) {
                    exchange.getResponseHeaders().add(HttpString.tryFromString(h.getName()), el.getValue());
                }
            }
            res.getEntity().writeTo(exchange.getOutputStream());
            exchange.endExchange();
        } catch (IOException e) {
            log.warn("Unable to make proxy call: {}", e.getMessage(), e);
            throw new InternalServerError(e);
        }
    }

}
