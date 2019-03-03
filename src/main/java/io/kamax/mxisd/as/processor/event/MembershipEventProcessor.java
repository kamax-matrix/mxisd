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

package io.kamax.mxisd.as.processor.event;

import com.google.gson.JsonObject;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix._ThreePid;
import io.kamax.matrix.client.as.MatrixApplicationServiceClient;
import io.kamax.matrix.event.EventKey;
import io.kamax.matrix.hs._MatrixRoom;
import io.kamax.mxisd.as.EventTypeProcessor;
import io.kamax.mxisd.backend.sql.synapse.Synapse;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.invitation.IMatrixIdInvite;
import io.kamax.mxisd.invitation.MatrixIdInvite;
import io.kamax.mxisd.notification.NotificationManager;
import io.kamax.mxisd.profile.ProfileManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MembershipEventProcessor implements EventTypeProcessor {

    private final static Logger log = LoggerFactory.getLogger(MembershipEventProcessor.class);

    private MatrixApplicationServiceClient client;

    private final MxisdConfig cfg;
    private ProfileManager profiler;
    private NotificationManager notif;
    private Synapse synapse;

    public MembershipEventProcessor(
            MatrixApplicationServiceClient client,
            MxisdConfig cfg,
            ProfileManager profiler,
            NotificationManager notif,
            Synapse synapse
    ) {
        this.client = client;
        this.cfg = cfg;
        this.profiler = profiler;
        this.notif = notif;
        this.synapse = synapse;
    }

    @Override
    public void process(JsonObject ev, _MatrixID sender, String roomId) {
        JsonObject content = EventKey.Content.findObj(ev).orElseGet(() -> {
            log.debug("No content found, falling back to full object");
            return ev;
        });

        String targetId = EventKey.StateKey.getStringOrNull(ev);
        if (StringUtils.isBlank(targetId)) {
            log.warn("Invalid event: No invitee ID, skipping");
            return;
        }

        _MatrixID target = MatrixID.asAcceptable(targetId);
        if (!StringUtils.equals(target.getDomain(), cfg.getMatrix().getDomain())) {
            log.debug("Ignoring invite for {}: not a local user");
            return;
        }

        log.info("Got membership event from {} to {} for room {}", sender.getId(), targetId, roomId);

        boolean isForMainUser = StringUtils.equals(target.getLocalPart(), cfg.getAppsvc().getUser().getMain());
        boolean isForExpInvUser = StringUtils.equals(target.getLocalPart(), cfg.getAppsvc().getUser().getInviteExpired());
        boolean isUs = isForMainUser || isForExpInvUser;

        if (StringUtils.equals("join", EventKey.Membership.getStringOrNull(content))) {
            if (!isForMainUser) {
                log.warn("We joined the room {} for another identity as the main user, which is not supported. Leaving...", roomId);

                client.getUser(target.getLocalPart()).getRoom(roomId).tryLeave().ifPresent(err -> {
                    log.warn("Could not decline invite to room {}: {} - {}", roomId, err.getErrcode(), err.getError());
                });
            }
        } else if (StringUtils.equals("invite", EventKey.Membership.getStringOrNull(content))) {
            if (isForMainUser) {
                processForMainUser(roomId, sender);
            } else if (isForExpInvUser) {
                processForExpiredInviteUser(roomId, target);
            } else {
                processForUserIdInvite(roomId, sender, target);
            }
        } else if (StringUtils.equals("leave", EventKey.Membership.getStringOrNull(content))) {
            _MatrixRoom room = client.getRoom(roomId);
            if (!isUs && room.getJoinedUsers().size() == 1) {
                // TODO we need to find out if this is only us remaining and leave the room if so, using the right client for it
            }
        } else {
            log.debug("This is not an supported type of membership event, skipping");
        }
    }

    private void processForMainUser(String roomId, _MatrixID sender) {
        List<String> roles = profiler.getRoles(sender);
        if (Collections.disjoint(roles, cfg.getAppsvc().getFeature().getAdmin().getAllowedRoles())) {
            log.info("Sender does not have any of the required roles, denying");
            client.getRoom(roomId).tryLeave().ifPresent(err -> {
                log.warn("Could not decline invite to room {}: {} - {}", roomId, err.getErrcode(), err.getError());
            });
        } else {
            client.getRoom(roomId).tryJoin().ifPresent(err -> {
                log.warn("Could not join room {}: {} - {}", roomId, err.getErrcode(), err.getError());
                client.getRoom(roomId).tryLeave().ifPresent(err1 -> {
                    log.warn("Could not decline invite to room {} after failed join: {} - {}", roomId, err1.getErrcode(), err1.getError());
                });
            });
        }
    }

    private void processForExpiredInviteUser(String roomId, _MatrixID invitee) {
        client.getUser(invitee.getLocalPart()).getRoom(roomId).tryLeave().ifPresent(err -> {
            log.warn("Could not decline invite to room {}: {} - {}", roomId, err.getErrcode(), err.getError());
        });
    }

    private void processForUserIdInvite(String roomId, _MatrixID sender, _MatrixID invitee) {
        String inviteeId = invitee.getId();

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
                log.info("Unable to fetch room name: Did you integrate your Homeserver as documented?");
            }

            IMatrixIdInvite inv = new MatrixIdInvite(roomId, sender, invitee, tpid.getMedium(), tpid.getAddress(), properties);
            notif.sendForInvite(inv);
            log.info("Notification for invite of {} sent to {}", inviteeId, tpid.getAddress());
            wasSent = true;
        }

        log.info("Was notification sent? {}", wasSent);
    }

}
