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

package io.kamax.mxisd.as;

import com.google.gson.JsonObject;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.event.EventKey;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.notification.NotificationManager;
import io.kamax.mxisd.profile.ProfileManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class AppServiceHandler {

    private final Logger log = LoggerFactory.getLogger(AppServiceHandler.class);

    private MatrixConfig cfg;
    private ProfileManager profiler;
    private NotificationManager notif;

    @Autowired
    public AppServiceHandler(MatrixConfig cfg, ProfileManager profiler, NotificationManager notif) {
        this.cfg = cfg;
        this.profiler = profiler;
        this.notif = notif;
    }

    public void processTransaction(List<JsonObject> eventsJson) {
        eventsJson.forEach(ev -> {
            if (!StringUtils.equals("m.room.member", GsonUtil.getStringOrNull(ev, "type"))) {
                return;
            }

            if (!StringUtils.equals("invite", GsonUtil.getStringOrNull(ev, "membership"))) {
                return;
            }

            String roomId = GsonUtil.getStringOrNull(ev, "room_id");
            _MatrixID sender = MatrixID.asAcceptable(GsonUtil.getStringOrNull(ev, "sender"));
            EventKey.StateKey.findString(ev).ifPresent(id -> {
                log.info("Got invite for {}", id);
                _MatrixID mxid = MatrixID.asAcceptable(id);
                if (!StringUtils.equals(mxid.getDomain(), cfg.getDomain())) {
                    log.info("Ignoring invite for {}: not a local user");
                    return;
                }

                profiler.getThreepids(mxid).forEach(tpid -> {
                    if (!StringUtils.equals("email", tpid.getMedium())) {
                        return;
                    }

                    log.info("Found an email address to notify about room invitation: {}", tpid.getAddress());
                    IMatrixIdInvite inv = new MatrixIdInvite(roomId, sender, mxid, tpid.getMedium(), tpid.getAddress(), new HashMap<>());
                    notif.sendForInvite(inv);
                });
            });
        });
    }

}
