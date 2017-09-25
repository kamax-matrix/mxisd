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

package io.kamax.mxisd.notification;

import io.kamax.mxisd.exception.NotImplementedException;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import io.kamax.mxisd.threepid.session.IThreePidSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationManager {

    private Map<String, INotificationHandler> handlers;

    @Autowired
    public NotificationManager(List<INotificationHandler> handlers) {
        this.handlers = new HashMap<>();
        handlers.forEach(h -> this.handlers.put(h.getMedium(), h));
    }

    private INotificationHandler ensureMedium(String medium) {
        INotificationHandler handler = handlers.get(medium);
        if (handler == null) {
            throw new NotImplementedException(medium + " is not a supported 3PID medium type");
        }

        return handler;
    }

    public boolean isMediumSupported(String medium) {
        return handlers.containsKey(medium);
    }

    public void sendForInvite(IThreePidInviteReply invite) {
        ensureMedium(invite.getInvite().getMedium()).sendForInvite(invite);
    }

    public void sendForValidation(IThreePidSession session) {
        ensureMedium(session.getThreePid().getMedium()).sendForValidation(session);
    }

    public void sendforRemoteValidation(IThreePidSession session) {
        ensureMedium(session.getThreePid().getMedium()).sendForRemoteValidation(session);
    }

}
