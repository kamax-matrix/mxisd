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
import io.kamax.mxisd.lookup.SingleLookupReply;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class LookupCommandProcessor implements CommandProcessor {

    public static final String Command = "lookup";

    @Override
    public void process(Mxisd m, _MatrixClient client, _MatrixRoom room, CommandLine cmdLine) {
        if (cmdLine.getArgList().size() != 3) {
            room.sendNotice(getUsage());
            return;
        }

        String medium = cmdLine.getArgList().get(1);
        String address = cmdLine.getArgList().get(2);
        if (StringUtils.isAnyBlank(medium, address)) {
            room.sendNotice(getUsage());
            return;
        }

        room.sendNotice("Processing...");
        Optional<SingleLookupReply> r = m.getIdentity().find(medium, address, true);
        if (!r.isPresent()) {
            room.sendNotice("No result");
            return;
        }

        SingleLookupReply lookup = r.get();
        StrBuilder b = new StrBuilder();
        b.append("Result for 3PID lookup of ").append(medium).append(" ").appendln(address).appendNewLine();
        b.append("Matrix ID: ").appendln(lookup.getMxid().getId());
        b.appendln("Validity:")
                .append("  Not Before: ").appendln(lookup.getNotBefore())
                .append("  Not After: ").appendln(lookup.getNotAfter());
        b.appendln("Signatures:");
        lookup.getSignatures().forEach((host, signs) -> {
            b.append("  ").append(host).appendln(":");
            signs.forEach((key, sign) -> b.append("    ").append(key).append(" -> ").appendln("OK"));
        });

        room.sendNotice(b.toString());
    }

    public String getUsage() {
        return "lookup MEDIUM ADDRESS";
    }

}
