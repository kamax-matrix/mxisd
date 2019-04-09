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
import io.kamax.matrix._MatrixID;
import io.kamax.matrix._MatrixUserProfile;
import io.kamax.matrix.client.as.MatrixApplicationServiceClient;
import io.kamax.matrix.hs._MatrixRoom;
import io.kamax.matrix.json.event.MatrixJsonRoomMessageEvent;
import io.kamax.mxisd.Mxisd;
import io.kamax.mxisd.as.processor.command.CommandProcessor;
import io.kamax.mxisd.as.processor.command.InviteCommandProcessor;
import io.kamax.mxisd.as.processor.command.LookupCommandProcessor;
import io.kamax.mxisd.as.processor.command.PingCommandProcessor;
import org.apache.commons.cli.*;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MessageEventProcessor implements EventTypeProcessor {

    private static final Logger log = LoggerFactory.getLogger(MessageEventProcessor.class);

    private final Mxisd m;
    private final MatrixApplicationServiceClient client;
    private Map<String, CommandProcessor> processors;

    public MessageEventProcessor(Mxisd m, MatrixApplicationServiceClient client) {
        this.m = m;
        this.client = client;

        processors = new HashMap<>();
        processors.put("?", (m1, client1, room, cmdLine) -> room.sendNotice(getHelp()));
        processors.put("help", (m1, client1, room, cmdLine) -> room.sendNotice(getHelp()));
        processors.put(PingCommandProcessor.Command, new PingCommandProcessor());
        processors.put(InviteCommandProcessor.Command, new InviteCommandProcessor());
        processors.put(LookupCommandProcessor.Command, new LookupCommandProcessor());
    }

    @Override
    public void process(JsonObject ev, _MatrixID sender, String roomId) {
        MatrixJsonRoomMessageEvent msgEv = new MatrixJsonRoomMessageEvent(ev);
        if (StringUtils.equals("m.notice", msgEv.getBodyType())) {
            log.info("Ignoring automated message");
            return;
        }

        _MatrixRoom room = client.getRoom(roomId);

        if (!m.getProfile().hasAnyRole(sender, m.getConfig().getAppsvc().getFeature().getAdmin().getAllowedRoles())) {
            room.sendNotice("You are not allowed to interact with me.");
            return;
        }

        List<_MatrixID> joinedUsers = room.getJoinedUsers().stream().map(_MatrixUserProfile::getId).collect(Collectors.toList());
        boolean joinedWithMainUser = joinedUsers.contains(client.getWhoAmI());
        boolean isAdminPrivate = joinedWithMainUser && joinedUsers.size() == 2;

        if (!StringUtils.equals("m.text", msgEv.getBodyType())) {
            log.info("Unsupported message event type: {}", msgEv.getBodyType());
            return;
        }

        String command = msgEv.getBody();
        if (!isAdminPrivate) {
            if (!StringUtils.startsWith(command, "!" + Mxisd.Name + " ")) {
                // Not for us
                return;
            }

            command = command.substring(("!" + Mxisd.Name + " ").length());
        }

        try {
            CommandLineParser p = new DefaultParser();
            CommandLine cmdLine = p.parse(new Options(), command.split(" ", 0));
            String cmd = cmdLine.getArgList().get(0);

            CommandProcessor cp = processors.get(cmd);
            if (Objects.isNull(cp)) {
                room.sendNotice("Unknown command: " + command + "\n\n" + getHelp());
            } else {
                cp.process(m, client, room, cmdLine);
            }
        } catch (ParseException e) {
            room.sendNotice("Invalid input" + "\n\n" + getHelp());
        } catch (RuntimeException e) {
            room.sendNotice("Error when running command: " + e.getMessage());
        }
    }

    public String getHelp() {
        StrBuilder builder = new StrBuilder();
        builder.appendln("Available commands:");
        for (String cmd : processors.keySet()) {
            builder.append("\t").appendln(cmd);
        }
        return builder.toString();
    }

}
