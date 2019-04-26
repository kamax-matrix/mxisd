/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2019 Kamax Sarl
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

import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.config.ViewConfig;
import io.kamax.mxisd.session.SessionManager;
import io.kamax.mxisd.session.ValidationResult;
import io.kamax.mxisd.util.FileUtil;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class SessionValidationGetHandler extends SessionValidateHandler {

    private transient final Logger log = LoggerFactory.getLogger(SessionValidationGetHandler.class);

    private ServerConfig srvCfg;
    private ViewConfig viewCfg;

    public SessionValidationGetHandler(SessionManager mgr, MxisdConfig cfg) {
        super(mgr);
        this.srvCfg = cfg.getServer();
        this.viewCfg = cfg.getView();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        log.info("Handling GET request to validate session");

        String sid = getQueryParameter(exchange, "sid");
        String secret = getQueryParameter(exchange, "client_secret");
        String token = getQueryParameter(exchange, "token");

        ValidationResult r = handleRequest(sid, secret, token);
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

}
