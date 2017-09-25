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
import io.kamax.matrix.MatrixID;
import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.controller.v1.io.ThreePidInviteReplyIO;
import io.kamax.mxisd.invitation.IThreePidInvite;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import io.kamax.mxisd.invitation.InvitationManager;
import io.kamax.mxisd.invitation.ThreePidInvite;
import io.kamax.mxisd.key.KeyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@CrossOrigin
@RequestMapping(path = IdentityAPIv1.BASE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
class InvitationController {

    private Logger log = LoggerFactory.getLogger(InvitationController.class);

    @Autowired
    private InvitationManager mgr;

    @Autowired
    private KeyManager keyMgr;

    @Autowired
    private ServerConfig srvCfg;

    private Gson gson = new Gson();

    @RequestMapping(value = "/store-invite", method = POST)
    String store(
            HttpServletRequest request,
            @RequestParam String sender,
            @RequestParam String medium,
            @RequestParam String address,
            @RequestParam("room_id") String roomId) {
        Map<String, String> parameters = new HashMap<>();
        for (String key : request.getParameterMap().keySet()) {
            parameters.put(key, request.getParameter(key));
        }
        IThreePidInvite invite = new ThreePidInvite(new MatrixID(sender), medium, address, roomId, parameters);
        IThreePidInviteReply reply = mgr.storeInvite(invite);

        return gson.toJson(new ThreePidInviteReplyIO(reply, keyMgr.getPublicKeyBase64(keyMgr.getCurrentIndex()), srvCfg.getPublicUrl()));
    }

}
