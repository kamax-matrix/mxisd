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

package io.kamax.mxisd.threepid.notification.email;

import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.config.threepid.medium.EmailConfig;
import io.kamax.mxisd.exception.ConfigurationException;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import io.kamax.mxisd.notification.INotificationHandler;
import io.kamax.mxisd.threepid.connector.email.IEmailConnector;
import io.kamax.mxisd.threepid.session.IThreePidSession;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmailNotificationHandler implements INotificationHandler {

    private EmailConfig cfg;
    private IEmailNotificationGenerator generator;
    private IEmailConnector connector;

    @Autowired
    public EmailNotificationHandler(EmailConfig cfg, List<IEmailNotificationGenerator> generators, List<IEmailConnector> connectors) {
        this.cfg = cfg;

        generator = generators.stream()
                .filter(o -> StringUtils.equals(cfg.getGenerator(), o.getId()))
                .findFirst()
                .orElseThrow(() -> new ConfigurationException("Email notification generator [" + cfg.getGenerator() + "] could not be found"));

        connector = connectors.stream()
                .filter(o -> StringUtils.equals(cfg.getConnector(), o.getId()))
                .findFirst()
                .orElseThrow(() -> new ConfigurationException("Email sender connector [" + cfg.getConnector() + "] could not be found"));
    }

    @Override
    public String getMedium() {
        return ThreePidMedium.Email.getId();
    }

    private void send(String recipient, String content) {
        connector.send(
                cfg.getIdentity().getFrom(),
                cfg.getIdentity().getName(),
                recipient,
                content
        );
    }

    @Override
    public void sendForInvite(IThreePidInviteReply invite) {
        send(invite.getInvite().getAddress(), generator.getForInvite(invite));
    }

    @Override
    public void sendForValidation(IThreePidSession session) {
        send(session.getThreePid().getAddress(), generator.getForValidation(session));
    }

    @Override
    public void sendForRemoteValidation(IThreePidSession session) {
        send(session.getThreePid().getAddress(), generator.getForRemoteValidation(session));
    }

}
