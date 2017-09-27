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

package io.kamax.mxisd.controller.identity.v1;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.ThreePid;
import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.config.ViewConfig;
import io.kamax.mxisd.controller.identity.v1.io.SessionEmailTokenRequestJson;
import io.kamax.mxisd.controller.identity.v1.io.SessionPhoneTokenRequestJson;
import io.kamax.mxisd.controller.identity.v1.io.SuccessStatusJson;
import io.kamax.mxisd.exception.BadRequestException;
import io.kamax.mxisd.exception.SessionNotValidatedException;
import io.kamax.mxisd.invitation.InvitationManager;
import io.kamax.mxisd.lookup.ThreePidValidation;
import io.kamax.mxisd.session.SessionMananger;
import io.kamax.mxisd.session.ValidationResult;
import io.kamax.mxisd.util.GsonParser;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@CrossOrigin
@RequestMapping(path = IdentityAPIv1.BASE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class SessionRestController {

    private Logger log = LoggerFactory.getLogger(SessionRestController.class);

    private class Sid { // FIXME replace with RequestTokenResponse

        private String sid;

        public Sid(String sid) {
            setSid(sid);
        }

        String getSid() {
            return sid;
        }

        void setSid(String sid) {
            this.sid = sid;
        }
    }

    @Autowired
    private ServerConfig srvCfg;

    @Autowired
    private SessionMananger mgr;

    @Autowired
    private InvitationManager invMgr;

    @Autowired
    private ViewConfig viewCfg;

    private Gson gson = new Gson();
    private GsonParser parser = new GsonParser(gson);

    @RequestMapping(value = "/validate/{medium}/requestToken")
    String init(HttpServletRequest request, HttpServletResponse response, @PathVariable String medium) throws IOException {
        log.info("Request {}: {}", request.getMethod(), request.getRequestURL(), request.getQueryString());
        if (ThreePidMedium.Email.is(medium)) {
            SessionEmailTokenRequestJson req = parser.parse(request, SessionEmailTokenRequestJson.class);
            return gson.toJson(new Sid(mgr.create(
                    request.getRemoteHost(),
                    new ThreePid(req.getMedium(), req.getValue()),
                    req.getSecret(),
                    req.getAttempt(),
                    req.getNextLink())));
        }

        if (ThreePidMedium.PhoneNumber.is(medium)) {
            SessionPhoneTokenRequestJson req = parser.parse(request, SessionPhoneTokenRequestJson.class);
            return gson.toJson(new Sid(mgr.create(
                    request.getRemoteHost(),
                    new ThreePid(req.getMedium(), req.getValue()),
                    req.getSecret(),
                    req.getAttempt(),
                    req.getNextLink())));
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("errcode", "M_INVALID_3PID_TYPE");
        obj.addProperty("error", medium + " is not supported as a 3PID type");
        response.setStatus(HttpStatus.SC_BAD_REQUEST);
        return gson.toJson(obj);
    }

    @RequestMapping(value = "/validate/{medium}/submitToken", method = POST)
    public String validate(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam String sid,
            @RequestParam("client_secret") String secret,
            @RequestParam String token,
            Model model
    ) {
        log.info("Requested: {}", request.getRequestURL());

        ValidationResult r = mgr.validate(sid, secret, token);
        log.info("Session {} was validated", sid);

        return gson.toJson(new SuccessStatusJson(true));
    }

    @RequestMapping(value = "/3pid/getValidated3pid")
    String check(HttpServletRequest request, HttpServletResponse response,
                 @RequestParam String sid, @RequestParam("client_secret") String secret) {
        log.info("Requested: {}", request.getRequestURL(), request.getQueryString());

        try {
            ThreePidValidation pid = mgr.getValidated(sid, secret);

            JsonObject obj = new JsonObject();
            obj.addProperty("medium", pid.getMedium());
            obj.addProperty("address", pid.getAddress());
            obj.addProperty("validated_at", pid.getValidation().toEpochMilli());

            return gson.toJson(obj);
        } catch (SessionNotValidatedException e) {
            log.info("Session {} was requested but has not yet been validated", sid);
            throw e;
        }
    }

    @RequestMapping(value = "/3pid/bind")
    String bind(HttpServletRequest request, HttpServletResponse response,
                @RequestParam String sid, @RequestParam("client_secret") String secret, @RequestParam String mxid) {
        log.info("Requested: {}", request.getRequestURL(), request.getQueryString());
        try {
            mgr.bind(sid, secret, mxid);
            return "{}";
        } catch (BadRequestException e) {
            log.info("requested session was not validated");

            JsonObject obj = new JsonObject();
            obj.addProperty("errcode", "M_SESSION_NOT_VALIDATED");
            obj.addProperty("error", e.getMessage());
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return gson.toJson(obj);
        } finally {
            // If a user registers, there is no standard login event. Instead, this is the only way to trigger
            // resolution at an appropriate time. Meh at synapse/Riot!
            invMgr.lookupMappingsForInvites();
        }
    }

}
