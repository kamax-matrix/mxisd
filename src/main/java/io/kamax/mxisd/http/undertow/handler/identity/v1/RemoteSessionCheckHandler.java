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

import io.kamax.mxisd.config.ViewConfig;
import io.kamax.mxisd.exception.SessionNotValidatedException;
import io.kamax.mxisd.http.undertow.handler.BasicHttpHandler;
import io.kamax.mxisd.session.SessionMananger;
import io.kamax.mxisd.util.FileUtil;
import io.undertow.server.HttpServerExchange;

public class RemoteSessionCheckHandler extends BasicHttpHandler {

    private SessionMananger mgr;
    private ViewConfig viewCfg;

    public RemoteSessionCheckHandler(SessionMananger mgr, ViewConfig viewCfg) {
        this.mgr = mgr;
        this.viewCfg = viewCfg;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String sid = getQueryParameter(exchange, "sid");
        String secret = getQueryParameter(exchange, "client_secret");

        String viewData;
        try {
            mgr.validateRemote(sid, secret);
            viewData = FileUtil.load(viewCfg.getSession().getRemote().getOnCheck().getSuccess());
        } catch (SessionNotValidatedException e) {
            viewData = FileUtil.load(viewCfg.getSession().getRemote().getOnCheck().getFailure());
        }

        writeBodyAsUtf8(exchange, viewData);
    }

}
