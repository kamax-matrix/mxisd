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

package io.kamax.mxisd.controller.v1

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.kamax.mxisd.controller.v1.io.SessionEmailTokenRequestJson
import io.kamax.mxisd.controller.v1.io.SessionPhoneTokenRequestJson
import io.kamax.mxisd.exception.BadRequestException
import io.kamax.mxisd.exception.NotImplementedException
import io.kamax.mxisd.lookup.ThreePid
import io.kamax.mxisd.mapping.MappingManager
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils
import org.apache.http.HttpStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets

@RestController
class SessionController {

    @Autowired
    private MappingManager mgr

    private Gson gson = new Gson()

    private Logger log = LoggerFactory.getLogger(SessionController.class)

    private <T> T fromJson(HttpServletRequest req, Class<T> obj) {
        gson.fromJson(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8), obj)
    }

    @RequestMapping(value = "/_matrix/identity/api/v1/validate/{medium}/requestToken")
    String init(HttpServletRequest request, HttpServletResponse response, @PathVariable String medium) {
        log.info("Requested: {}", request.getRequestURL(), request.getQueryString())

        if (StringUtils.equals("email", medium)) {
            SessionEmailTokenRequestJson req = fromJson(request, SessionEmailTokenRequestJson.class)
            return gson.toJson(new Sid(mgr.create(req)))
        }

        if (StringUtils.equals("msisdn", medium)) {
            SessionPhoneTokenRequestJson req = fromJson(request, SessionPhoneTokenRequestJson.class)
            return gson.toJson(new Sid(mgr.create(req)))
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("errcode", "M_INVALID_3PID_TYPE")
        obj.addProperty("error", medium + " is not supported as a 3PID type")
        response.setStatus(HttpStatus.SC_BAD_REQUEST)
        return gson.toJson(obj)
    }

    @RequestMapping(value = "/_matrix/identity/api/v1/validate/{medium}/submitToken")
    String validate(HttpServletRequest request) {
        log.info("Requested: {}?{}", request.getRequestURL(), request.getQueryString())

        throw new NotImplementedException()
    }

    @RequestMapping(value = "/_matrix/identity/api/v1/3pid/getValidated3pid")
    String check(HttpServletRequest request, HttpServletResponse response,
                 @RequestParam String sid, @RequestParam("client_secret") String secret) {
        log.info("Requested: {}?{}", request.getRequestURL(), request.getQueryString())

        Optional<ThreePid> result = mgr.getValidated(sid, secret)
        if (result.isPresent()) {
            log.info("requested session was validated")
            ThreePid pid = result.get()

            JsonObject obj = new JsonObject()
            obj.addProperty("medium", pid.getMedium())
            obj.addProperty("address", pid.getAddress())
            obj.addProperty("validated_at", pid.getValidation().toEpochMilli())

            return gson.toJson(obj);
        } else {
            log.info("requested session was not validated")

            JsonObject obj = new JsonObject()
            obj.addProperty("errcode", "M_SESSION_NOT_VALIDATED")
            obj.addProperty("error", "sid, secret or session not valid")
            response.setStatus(HttpStatus.SC_BAD_REQUEST)
            return gson.toJson(obj)
        }
    }

    @RequestMapping(value = "/_matrix/identity/api/v1/3pid/bind")
    String bind(HttpServletRequest request, HttpServletResponse response,
                @RequestParam String sid, @RequestParam("client_secret") String secret, @RequestParam String mxid) {
        String data = IOUtils.toString(request.getReader())
        log.info("Requested: {}", request.getRequestURL(), request.getQueryString())
        try {
            mgr.bind(sid, secret, mxid)
            return "{}"
        } catch (BadRequestException e) {
            log.info("requested session was not validated")

            obj = new JsonObject()
            obj.addProperty("errcode", "M_SESSION_NOT_VALIDATED")
            obj.addProperty("error", e.getMessage())
            response.setStatus(HttpStatus.SC_BAD_REQUEST)
            return gson.toJson(obj)
        }
    }

    private class Sid {

        private String sid;

        public Sid(String sid) {
            setSid(sid);
        }

        String getSid() {
            return sid
        }

        void setSid(String sid) {
            this.sid = sid
        }
    }

}
