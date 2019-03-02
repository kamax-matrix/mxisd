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

package io.kamax.mxisd.as.processor;

import com.google.gson.JsonObject;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix._MatrixUserProfile;
import io.kamax.matrix.client.as.MatrixApplicationServiceClient;
import io.kamax.matrix.hs._MatrixRoom;
import io.kamax.matrix.json.event.MatrixJsonRoomMessageEvent;
import io.kamax.mxisd.as.EventTypeProcessor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class MessageEventProcessor implements EventTypeProcessor {

    private static final Logger log = LoggerFactory.getLogger(MessageEventProcessor.class);

    private final MatrixApplicationServiceClient client;

    public MessageEventProcessor(MatrixApplicationServiceClient client) {
        this.client = client;
    }

    @Override
    public void process(JsonObject ev, _MatrixID sender, String roomId) {
        _MatrixRoom room = client.getRoom(roomId);
        List<_MatrixID> joinedUsers = room.getJoinedUsers().stream().map(_MatrixUserProfile::getId).collect(Collectors.toList());
        boolean joinedWithMainUser = joinedUsers.contains(client.getWhoAmI());
        boolean isAdminPrivate = joinedWithMainUser && joinedUsers.size() == 2;

        MatrixJsonRoomMessageEvent msgEv = new MatrixJsonRoomMessageEvent(ev);
        if (StringUtils.equals("m.notice", msgEv.getBodyType())) {
            log.info("Ignoring automated message");
            return;
        }

        if (!StringUtils.equals("m.text", msgEv.getBodyType())) {
            log.info("Unsupported message event type: {}", msgEv.getBodyType());
            return;
        }

        String command = msgEv.getBody();
        if (!isAdminPrivate) {
            if (StringUtils.equals(command, "!mxisd")) {
                // TODO show help
            }
            if (!StringUtils.startsWith(command, "!mxisd ")) {
                // Not for us
                return;
            }

            command = command.substring("!mxisd ".length());
        }

        if (StringUtils.equals("ping", command)) {
            room.sendText("Pong!");
        }
    }

}
