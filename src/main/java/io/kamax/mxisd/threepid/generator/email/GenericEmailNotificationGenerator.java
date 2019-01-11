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

package io.kamax.mxisd.threepid.generator.email;

import io.kamax.matrix.ThreePid;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.ServerConfig;
import io.kamax.mxisd.config.threepid.medium.EmailConfig;
import io.kamax.mxisd.config.threepid.medium.EmailTemplateConfig;
import io.kamax.mxisd.threepid.generator.GenericTemplateNotificationGenerator;
import org.apache.commons.lang3.StringUtils;

public class GenericEmailNotificationGenerator extends GenericTemplateNotificationGenerator implements EmailGenerator {

    public static final String ID = "template";

    private EmailConfig cfg;

    public GenericEmailNotificationGenerator(EmailTemplateConfig templateCfg, EmailConfig cfg, MatrixConfig mxCfg, ServerConfig srvCfg) {
        super(mxCfg, srvCfg, templateCfg.build());
        this.cfg = cfg;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected String populateForCommon(ThreePid recipient, String body) {
        body = super.populateForCommon(recipient, body);
        body = body.replace("%FROM_EMAIL%", StringUtils.defaultIfEmpty(cfg.getIdentity().getFrom(), ""));
        body = body.replace("%FROM_NAME%", StringUtils.defaultIfEmpty(cfg.getIdentity().getName(), ""));
        return body;
    }

}
