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

package io.kamax.mxisd.threepid.generator.phone;

import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.config.threepid.medium.PhoneSmsTemplateConfig;
import io.kamax.mxisd.threepid.generator.GenericTemplateNotificationGenerator;

public class SmsNotificationGenerator extends GenericTemplateNotificationGenerator implements PhoneGenerator {

    public static final String ID = "template";

    public SmsNotificationGenerator(MatrixConfig mxCfg, ServerConfig srvCfg, PhoneSmsTemplateConfig cfg) {
        super(mxCfg, srvCfg, cfg.build());
    }

    @Override
    public String getId() {
        return ID;
    }

}
