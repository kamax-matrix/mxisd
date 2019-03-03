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

package io.kamax.mxisd.as.processor.command;

import io.kamax.matrix.client._MatrixClient;
import io.kamax.matrix.hs._MatrixRoom;
import io.kamax.mxisd.Mxisd;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;

import java.util.List;

public class InviteCommandProcessor implements CommandProcessor {

    public static final String Command = "invite";

    @Override
    public void process(Mxisd m, _MatrixClient client, _MatrixRoom room, CommandLine cmdLine) {
        if (cmdLine.getArgs().length < 2) {
            room.sendNotice(buildHelp());
        } else {
            String arg = cmdLine.getArgList().get(1);
            String response;
            if (StringUtils.equals("list", arg)) {

                StrBuilder b = new StrBuilder();

                List<IThreePidInviteReply> invites = m.getInvite().listInvites();
                if (invites.isEmpty()) {
                    b.appendln("No invites!");
                    response = b.toString();
                } else {
                    b.appendln("Invites:");


                    for (IThreePidInviteReply invite : invites) {
                        b.appendNewLine().append("ID: ").append(invite.getId());
                        b.appendNewLine().append("Room: ").append(invite.getInvite().getRoomId());
                        b.appendNewLine().append("Medium: ").append(invite.getInvite().getMedium());
                        b.appendNewLine().append("Address: ").append(invite.getInvite().getAddress());
                        b.appendNewLine();
                    }

                    response = b.appendNewLine().append("Total: " + invites.size()).toString();
                }
            } else if (StringUtils.equals("show", arg)) {
                if (cmdLine.getArgList().size() < 3) {
                    response = buildHelp();
                } else {
                    String id = cmdLine.getArgList().get(2);
                    IThreePidInviteReply invite = m.getInvite().getInvite(id);
                    StrBuilder b = new StrBuilder();
                    b.appendln("Details for Invitation #" + id);
                    b.appendNewLine().append("Room: ").append(invite.getInvite().getRoomId());
                    b.appendNewLine().append("Sender: ").append(invite.getInvite().getSender().toString());
                    b.appendNewLine().append("Medium: ").append(invite.getInvite().getMedium());
                    b.appendNewLine().append("Address: ").append(invite.getInvite().getAddress());
                    b.appendNewLine().append("Display name: ").append(invite.getDisplayName());
                    b.appendNewLine().appendNewLine().append("Properties:");
                    invite.getInvite().getProperties().forEach((k, v) -> {
                        b.appendNewLine().append("\t").append(k).append("=").append(v);
                    });
                    b.appendNewLine();

                    response = b.toString();
                }
            } else if (StringUtils.equals("revoke", arg)) {
                if (cmdLine.getArgList().size() < 3) {
                    response = buildHelp();
                } else {
                    m.getInvite().expireInvite(cmdLine.getArgList().get(2));
                    response = "OK";
                }
            } else {
                response = buildError("Unknown invite action: " + arg, true);
            }

            room.sendNotice(response);
        }
    }

    private String buildError(String message, boolean showHelp) {
        if (showHelp) {
            message = message + "\n\n" + buildHelp();
        }

        return message;
    }

    private String buildHelp() {
        return "Available actions:\n\n" +
                "list - List invites\n" +
                "show ID - Show detailed info about a specific invite\n" +
                "revoke ID - Revoke a pending invite by resolving it to the configured Expiration user\n";
    }

}
