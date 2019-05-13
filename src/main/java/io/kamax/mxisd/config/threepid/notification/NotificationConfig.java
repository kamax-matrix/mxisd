/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Kamax Sarl
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

package io.kamax.mxisd.config.threepid.notification;

import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.threepid.notification.email.EmailRawNotificationHandler;
import io.kamax.mxisd.threepid.notification.phone.PhoneNotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class NotificationConfig {

    private transient final Logger log = LoggerFactory.getLogger(NotificationConfig.class);

    private Map<String, String> handler = new HashMap<>();
    private Map<String, Object> handlers = new HashMap<>();

    public NotificationConfig() {
        handler.put(ThreePidMedium.Email.getId(), EmailRawNotificationHandler.ID);
        handler.put(ThreePidMedium.PhoneNumber.getId(), PhoneNotificationHandler.ID);
    }

    public Map<String, String> getHandler() {
        return handler;
    }

    public void setHandler(Map<String, String> handler) {
        this.handler = handler;
    }

    public Map<String, Object> getHandlers() {
        return handlers;
    }

    public void setHandlers(Map<String, Object> handlers) {
        this.handlers = handlers;
    }

    public void build() {
        log.info("--- Notification config ---");
        log.info("Handlers:");
        handler.forEach((k, v) -> log.info("  {}: {}", k, v));
    }

}
