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
import com.google.gson.JsonObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class StatusController {

    private Gson gson = new Gson();

    @RequestMapping(value = "/_matrix/identity/status")
    public String getStatus() {
        // TODO link to backend
        JsonObject status = new JsonObject();
        status.addProperty("health", "OK");

        JsonObject obj = new JsonObject();
        obj.add("status", status);

        return gson.toJson(obj);
    }

}
