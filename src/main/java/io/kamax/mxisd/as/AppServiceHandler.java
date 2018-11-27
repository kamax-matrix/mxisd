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
import io.kamax.matrix.ThreePidMedium;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix._ThreePid;
import io.kamax.matrix.event.EventKey;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.backend.sql.synapse.Synapse;
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
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AppServiceHandler {

    private final Logger log = LoggerFactory.getLogger(AppServiceHandler.class);

    private MatrixConfig cfg;
    private ProfileManager profiler;
    private NotificationManager notif;
    private Synapse synapse;

    @Autowired
    public AppServiceHandler(MatrixConfig cfg, ProfileManager profiler, NotificationManager notif, Synapse synapse) {
        this.cfg = cfg;
        this.profiler = profiler;
        this.notif = notif;
        this.synapse = synapse;
    }

    public void processTransaction(List<JsonObject> eventsJson) {
        eventsJson.forEach(ev -> {
            String evId = EventKey.Id.getStringOrNull(ev);
            if (StringUtils.isBlank(evId)) {
                log.warn("Event has no ID, skipping");
                log.debug("Event:\n{}", GsonUtil.getPrettyForLog(ev));
                return;
            }
            log.debug("Event {}: processing start", evId);

            String roomId = EventKey.RoomId.getStringOrNull(ev);
            if (StringUtils.isBlank(roomId)) {
                log.debug("Event has no room ID, skipping");
                return;
            }

            String senderId = EventKey.Sender.getStringOrNull(ev);
            if (StringUtils.isBlank(senderId)) {
                log.debug("Event has no room ID, skipping");
                return;
            }
            _MatrixID sender = MatrixID.asAcceptable(senderId);

            if (!StringUtils.equals("m.room.member", GsonUtil.getStringOrNull(ev, "type"))) {
                log.debug("This is not a room membership event, skipping");
                return;
            }

            if (!StringUtils.equals("invite", GsonUtil.getStringOrNull(ev, "membership"))) {
                log.debug("This is not an invite event, skipping");
                return;
            }

            String inviteeId = EventKey.StateKey.getStringOrNull(ev);
            if (StringUtils.isBlank(inviteeId)) {
                log.warn("Invalid event: No invitee ID, skipping");
                return;
            }

            _MatrixID invitee = MatrixID.asAcceptable(inviteeId);
            if (!StringUtils.equals(invitee.getDomain(), cfg.getDomain())) {
                log.debug("Ignoring invite for {}: not a local user");
                return;
            }

            log.info("Got invite for {}", inviteeId);

            boolean wasSent = false;
            List<_ThreePid> tpids = profiler.getThreepids(invitee).stream()
                    .filter(tpid -> ThreePidMedium.Email.is(tpid.getMedium()))
                    .collect(Collectors.toList());
            log.info("Found {} email(s) in identity store for {}", tpids.size(), inviteeId);

            for (_ThreePid tpid : tpids) {
                log.info("Found Email to notify about room invitation: {}", tpid.getAddress());
                Map<String, String> properties = new HashMap<>();
                profiler.getDisplayName(sender).ifPresent(name -> properties.put("sender_display_name", name));
                try {
                    synapse.getRoomName(roomId).ifPresent(name -> properties.put("room_name", name));
                } catch (RuntimeException e) {
                    log.warn("Could not fetch room name", e);
                    log.warn("Unable to fetch room name: Did you integrate your Homeserver as documented?");
                }

                IMatrixIdInvite inv = new MatrixIdInvite(roomId, sender, invitee, tpid.getMedium(), tpid.getAddress(), properties);
                notif.sendForInvite(inv);
                log.info("Notification for invite of {} sent to {}", inviteeId, tpid.getAddress());
                wasSent = true;
            }

            log.info("Was notification sent? {}", wasSent);

            log.debug("Event {}: processing end", evId);
        });
    }

}
