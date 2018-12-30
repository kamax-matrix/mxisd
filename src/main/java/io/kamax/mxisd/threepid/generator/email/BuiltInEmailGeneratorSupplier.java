/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
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

import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.Mxisd;
import io.kamax.mxisd.config.threepid.medium.EmailConfig;
import io.kamax.mxisd.config.threepid.medium.EmailTemplateConfig;
import org.apache.commons.lang.StringUtils;

import java.util.Optional;

public class BuiltInEmailGeneratorSupplier implements EmailGeneratorSupplier {

    private boolean processed = false;
    private EmailGenerator obj;

    @Override
    public Optional<EmailGenerator> apply(EmailConfig emailConfig, Mxisd mxisd) {
        if (!processed) {
            if (StringUtils.equals(GenericEmailNotificationGenerator.ID, emailConfig.getGenerator())) {
                EmailTemplateConfig cfg = Optional.ofNullable(emailConfig.getGenerators().get(GenericEmailNotificationGenerator.ID))
                        .map(json -> GsonUtil.get().fromJson(json, EmailTemplateConfig.class))
                        .orElseGet(EmailTemplateConfig::new);
                obj = new GenericEmailNotificationGenerator(cfg, emailConfig, mxisd.getConfig().getMatrix(), mxisd.getConfig().getServer());
            }
        }

        processed = true;
        return Optional.ofNullable(obj);
    }

}
