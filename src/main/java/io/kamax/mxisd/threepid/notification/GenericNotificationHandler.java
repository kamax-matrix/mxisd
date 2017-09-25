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

package io.kamax.mxisd.threepid.notification;

import io.kamax.mxisd.exception.ConfigurationException;
import io.kamax.mxisd.invitation.IThreePidInviteReply;
import io.kamax.mxisd.notification.INotificationHandler;
import io.kamax.mxisd.threepid.connector.IThreePidConnector;
import io.kamax.mxisd.threepid.session.IThreePidSession;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public abstract class GenericNotificationHandler<A extends IThreePidConnector, B extends INotificationGenerator> implements INotificationHandler {

    private A connector;
    private B generator;

    protected abstract String getConnectorId();

    protected abstract String getGeneratorId();

    protected abstract void send(A connector, String recipient, String content);

    protected void process(List<A> connectors, List<B> generators) {
        generator = generators.stream()
                .filter(o -> StringUtils.equals(getGeneratorId(), o.getId()))
                .findFirst()
                .orElseThrow(() -> new ConfigurationException(getMedium() + " notification generator [" +
                        getGeneratorId() + "] could not be found"));

        connector = connectors.stream()
                .filter(o -> StringUtils.equals(getConnectorId(), o.getId()))
                .findFirst()
                .orElseThrow(() -> new ConfigurationException(getMedium() + " sender connector [" +
                        getConnectorId() + "] could not be found"));
    }

    @Override
    public void sendForInvite(IThreePidInviteReply invite) {
        send(connector, invite.getInvite().getAddress(), generator.getForInvite(invite));
    }

    @Override
    public void sendForValidation(IThreePidSession session) {
        send(connector, session.getThreePid().getAddress(), generator.getForValidation(session));
    }

    @Override
    public void sendForRemoteValidation(IThreePidSession session) {
        send(connector, session.getThreePid().getAddress(), generator.getForRemoteValidation(session));
    }

}
