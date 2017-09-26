/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
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

package io.kamax.mxisd.controller.v1;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kamax.mxisd.auth.AuthManager;
import io.kamax.mxisd.auth.UserAuthResult;
import io.kamax.mxisd.controller.v1.io.CredentialsValidationResponse;
import io.kamax.mxisd.exception.JsonMemberNotFoundException;
import io.kamax.mxisd.util.GsonParser;
import io.kamax.mxisd.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@CrossOrigin
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class AuthController {

    private Logger log = LoggerFactory.getLogger(AuthController.class);

    private Gson gson = GsonUtil.build();
    private GsonParser parser = new GsonParser(gson);

    @Autowired
    private AuthManager mgr;

    @RequestMapping(value = "/_matrix-internal/identity/v1/check_credentials", method = RequestMethod.POST)
    public String checkCredentials(HttpServletRequest req) {
        try {
            JsonObject authData = parser.parse(req.getInputStream(), "user");
            if (!authData.has("id") || !authData.has("password")) {
                throw new JsonMemberNotFoundException("Missing id or password keys");
            }

            String id = authData.get("id").getAsString();
            log.info("Requested to check credentials for {}", id);
            String password = authData.get("password").getAsString();

            UserAuthResult result = mgr.authenticate(id, password);
            CredentialsValidationResponse response = new CredentialsValidationResponse(result.isSuccess());

            if (result.isSuccess()) {
                response.setDisplayName(result.getDisplayName());
                response.getProfile().setThreePids(result.getThreePids());
            }
            JsonElement authObj = gson.toJsonTree(response);

            JsonObject obj = new JsonObject();
            obj.add("auth", authObj);
            obj.add("authentication", authObj); // TODO remove later, legacy support
            return gson.toJson(obj);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
