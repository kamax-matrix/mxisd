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

package io.kamax.mxisd.http.undertow.handler.identity.v1;

import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.config.ViewConfig;
import io.kamax.mxisd.http.IsAPIv1;
import io.kamax.mxisd.http.io.identity.SuccessStatusJson;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.session.SessionManager;
import io.kamax.mxisd.session.ValidationResult;
import io.kamax.mxisd.util.FileUtil;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class SessionValidateHandler extends BasicHttpHandler {

    public static final String Path = IsAPIv1.Base + "/validate/{medium}/submitToken";

    private transient final Logger log = LoggerFactory.getLogger(SessionValidateHandler.class);

    private SessionManager mgr;
    private ServerConfig srvCfg;
    private ViewConfig viewCfg;

    public SessionValidateHandler(SessionManager mgr, ServerConfig srvCfg, ViewConfig viewCfg) {
        this.mgr = mgr;
        this.srvCfg = srvCfg;
        this.viewCfg = viewCfg;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        String medium = getQueryParameter(exchange, "medium");
        String sid = getQueryParameter(exchange, "sid");
        String secret = getQueryParameter(exchange, "client_secret");
        String token = getQueryParameter(exchange, "token");

        boolean isHtmlRequest = false;
        for (String v : exchange.getRequestHeaders().get("Accept")) {
            if (StringUtils.startsWithIgnoreCase(v, "text/html")) {
                isHtmlRequest = true;
                break;
            }
        }

        if (isHtmlRequest) {
            handleHtmlRequest(exchange, medium, sid, secret, token);
        } else {
            handleJsonRequest(exchange, sid, secret, token);
        }
    }

    private void handleHtmlRequest(HttpServerExchange exchange, String medium, String sid, String secret, String token) {
        log.info("Validating session {} for medium {}", sid, medium);
        ValidationResult r = mgr.validate(sid, secret, token);
        log.info("Session {} was validated", sid);
        if (r.getNextUrl().isPresent()) {
            String url = r.getNextUrl().get();
            try {
                url = new URL(url).toString();
            } catch (MalformedURLException e) {
                log.info("Session next URL {} is not a valid one, will prepend public URL {}", url, srvCfg.getPublicUrl());
                url = srvCfg.getPublicUrl() + r.getNextUrl().get();
            }
            log.info("Session {} validation: next URL is present, redirecting to {}", sid, url);
            exchange.setStatusCode(302);
            exchange.getResponseHeaders().add(HttpString.tryFromString("Location"), url);
        } else {
            try {
                String data = FileUtil.load(viewCfg.getSession().getOnTokenSubmit().getSuccess());
                writeBodyAsUtf8(exchange, data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleJsonRequest(HttpServerExchange exchange, String sid, String secret, String token) {
        log.info("Requested: {}", exchange.getRequestURL());

        mgr.validate(sid, secret, token);
        log.info("Session {} was validated", sid);

        respondJson(exchange, new SuccessStatusJson(true));
    }

}
