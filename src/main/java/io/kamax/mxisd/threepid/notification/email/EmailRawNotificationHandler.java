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

package io.kamax.mxisd.threepid.notification.email;

import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.config.threepid.medium.EmailConfig;
import io.kamax.mxisd.threepid.connector.email.EmailConnector;
import io.kamax.mxisd.threepid.generator.email.EmailGenerator;
import io.kamax.mxisd.threepid.notification.GenericNotificationHandler;

import java.util.List;

public class EmailRawNotificationHandler extends GenericNotificationHandler<EmailConnector, EmailGenerator> {

    public static final String ID = "raw";

    private EmailConfig cfg;

    public EmailRawNotificationHandler(EmailConfig cfg, List<EmailGenerator> generators, List<EmailConnector> connectors) {
        this.cfg = cfg;
        process(connectors, generators);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getMedium() {
        return ThreePidMedium.Email.getId();
    }

    @Override
    protected String getConnectorId() {
        return cfg.getConnector();
    }

    @Override
    protected String getGeneratorId() {
        return cfg.getGenerator();
    }

    @Override
    protected void send(EmailConnector connector, String recipient, String content) {
        connector.send(
                cfg.getIdentity().getFrom(),
                cfg.getIdentity().getName(),
                recipient,
                content
        );
    }

}
