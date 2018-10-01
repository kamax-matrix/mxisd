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

package io.kamax.mxisd.controller.app.v1;

import com.google.gson.JsonObject;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.as.AppServiceHandler;
import io.kamax.mxisd.config.ListenerConfig;
import io.kamax.mxisd.exception.HttpMatrixException;
import io.kamax.mxisd.exception.NotAllowedException;
import io.kamax.mxisd.util.GsonParser;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RestController
@CrossOrigin
@RequestMapping(path = "/_matrix/app/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class AppServiceController {

    private final Logger log = LoggerFactory.getLogger(AppServiceController.class);
    private final ListenerConfig cfg;

    private final String notFoundBody;
    private final GsonParser parser;
    private final AppServiceHandler handler;

    @Autowired
    public AppServiceController(ListenerConfig cfg, AppServiceHandler handler) {
        this.notFoundBody = GsonUtil.get().toJson(GsonUtil.makeObj("errcode", "io.kamax.mxisd.AS_NOT_FOUND"));
        this.parser = new GsonParser();

        this.cfg = cfg;
        this.handler = handler;
    }

    private void validateToken(String token) {
        if (StringUtils.isBlank(token)) {
            throw new HttpMatrixException(401, "M_UNAUTHORIZED", "No HS token");
        }

        if (!StringUtils.equals(cfg.getToken().getHs(), token)) {
            throw new NotAllowedException("Invalid HS token");
        }
    }

    @RequestMapping(value = "/rooms/**", method = GET)
    public String getRoom(HttpServletResponse res, @RequestParam(name = "access_token", required = false) String token) {
        validateToken(token);

        res.setStatus(404);
        return notFoundBody;
    }

    @RequestMapping(value = "/users/**", method = GET)
    public String getUser(HttpServletResponse res, @RequestParam(name = "access_token", required = false) String token) {
        validateToken(token);

        res.setStatus(404);
        return notFoundBody;
    }

    @RequestMapping(value = "/transactions/{txnId:.+}", method = PUT)
    public Object getTransaction(
            HttpServletRequest request,
            @RequestParam(name = "access_token", required = false) String token,
            @PathVariable String txnId) {
        try {
            validateToken(token);

            log.info("Processing transaction {}", txnId);
            List<JsonObject> events = GsonUtil.asList(GsonUtil.getArray(parser.parse(request.getInputStream()), "events"), JsonObject.class);
            handler.processTransaction(events);
            return "{}";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
