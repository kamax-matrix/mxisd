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
import org.apache.commons.lang.StringUtils;

public class InviteCommandProcessor implements CommandProcessor {

    public static final String Command = "invite";

    @Override
    public void process(Mxisd m, _MatrixClient client, _MatrixRoom room, String command, String[] arguments) {
        if (arguments.length < 1) {
            room.sendText(buildHelp());
        }

        String subcmd = arguments[0];
        String response;
        if (StringUtils.equals("list", subcmd)) {
            response = buildError("This command is not supported yet", false);
        } else if (StringUtils.endsWith("show", subcmd)) {
            response = buildError("This command is not supported yet", false);
        } else if (StringUtils.equals("revoke", subcmd)) {
            response = buildError("This command is not supported yet", false);
        } else {
            response = buildError("Unknown command: " + subcmd, true);
        }

        room.sendText(response);
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
                "show - Show detailed info about a specific invite\n" +
                "revoke - Revoke a pending invite by resolving it to the configured Expiration user\n";
    }

}
