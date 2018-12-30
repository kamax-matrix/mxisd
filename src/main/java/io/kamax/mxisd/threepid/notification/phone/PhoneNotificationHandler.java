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

package io.kamax.mxisd.threepid.notification.phone;

import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.config.threepid.medium.PhoneConfig;
import io.kamax.mxisd.threepid.connector.phone.PhoneConnector;
import io.kamax.mxisd.threepid.generator.phone.PhoneGenerator;
import io.kamax.mxisd.threepid.notification.GenericNotificationHandler;

import java.util.List;

public class PhoneNotificationHandler extends GenericNotificationHandler<PhoneConnector, PhoneGenerator> {

    public static final String ID = "raw";

    private PhoneConfig cfg;

    public PhoneNotificationHandler(PhoneConfig cfg, List<PhoneGenerator> generators, List<PhoneConnector> connectors) {
        this.cfg = cfg;
        process(connectors, generators);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getMedium() {
        return ThreePidMedium.PhoneNumber.getId();
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
    protected void send(PhoneConnector connector, String recipient, String content) {
        connector.send(recipient, content);
    }

}
