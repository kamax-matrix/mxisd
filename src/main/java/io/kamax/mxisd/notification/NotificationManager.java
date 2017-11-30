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

import io.kamax.mxisd.config.threepid.notification.NotificationConfig;
import io.kamax.mxisd.exception.NotImplementedException;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import io.kamax.mxisd.threepid.session.IThreePidSession;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationManager {

    private Logger log = LoggerFactory.getLogger(NotificationManager.class);

    private Map<String, INotificationHandler> handlers;

    @Autowired
    public NotificationManager(NotificationConfig cfg, List<INotificationHandler> handlers) {
        this.handlers = new HashMap<>();
        handlers.forEach(h -> {
            log.info("Found handler {} for medium {}", h.getId(), h.getMedium());
            String handlerId = cfg.getHandler().getOrDefault(h.getMedium(), "raw");
            if (StringUtils.equals(handlerId, h.getId())) {
                this.handlers.put(h.getMedium(), h);
            }
        });

        log.info("--- Notification handler ---");
        this.handlers.forEach((k, v) -> log.info("\tHandler for {}: {}", k, v.getId()));
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
