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

package io.kamax.mxisd.threepid.notification;

import com.google.gson.JsonSyntaxException;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.Mxisd;
import io.kamax.mxisd.config.threepid.connector.EmailSendGridConfig;
import io.kamax.mxisd.config.threepid.medium.EmailConfig;
import io.kamax.mxisd.config.threepid.medium.PhoneConfig;
import io.kamax.mxisd.exception.ConfigurationException;
import io.kamax.mxisd.notification.NotificationHandlerSupplier;
import io.kamax.mxisd.notification.NotificationHandlers;
import io.kamax.mxisd.threepid.connector.email.EmailConnector;
import io.kamax.mxisd.threepid.connector.email.EmailConnectorSupplier;
import io.kamax.mxisd.threepid.connector.phone.PhoneConnector;
import io.kamax.mxisd.threepid.connector.phone.PhoneConnectorSupplier;
import io.kamax.mxisd.threepid.generator.email.EmailGenerator;
import io.kamax.mxisd.threepid.generator.email.EmailGeneratorSupplier;
import io.kamax.mxisd.threepid.generator.phone.PhoneGenerator;
import io.kamax.mxisd.threepid.generator.phone.PhoneGeneratorSupplier;
import io.kamax.mxisd.threepid.notification.email.EmailRawNotificationHandler;
import io.kamax.mxisd.threepid.notification.email.EmailSendGridNotificationHandler;
import io.kamax.mxisd.threepid.notification.phone.PhoneNotificationHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class BuiltInNotificationHandlerSupplier implements NotificationHandlerSupplier {

    @Override
    public void accept(Mxisd mxisd) {
        String emailHandler = mxisd.getConfig().getNotification().getHandler().get(ThreePidMedium.Email.getId());
        acceptEmail(emailHandler, mxisd);

        String phoneHandler = mxisd.getConfig().getNotification().getHandler().get(ThreePidMedium.PhoneNumber.getId());
        acceptPhone(phoneHandler, mxisd);
    }

    private void acceptEmail(String handler, Mxisd mxisd) {
        if (StringUtils.equals(EmailRawNotificationHandler.ID, handler)) {
            Object o = mxisd.getConfig().getThreepid().getMedium().get(ThreePidMedium.Email.getId());
            if (Objects.nonNull(o)) {
                EmailConfig emailCfg;
                try {
                    emailCfg = GsonUtil.get().fromJson(GsonUtil.makeObj(o), EmailConfig.class);
                } catch (JsonSyntaxException e) {
                    throw new ConfigurationException("Invalid configuration for threepid email notification");
                }

                if (StringUtils.isBlank(emailCfg.getGenerator())) {
                    throw new ConfigurationException("notification.email.generator");
                }

                if (StringUtils.isBlank(emailCfg.getConnector())) {
                    throw new ConfigurationException("notification.email.connector");
                }

                List<EmailGenerator> generators = StreamSupport
                        .stream(ServiceLoader.load(EmailGeneratorSupplier.class).spliterator(), false)
                        .map(s -> s.apply(emailCfg, mxisd))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

                List<EmailConnector> connectors = StreamSupport
                        .stream(ServiceLoader.load(EmailConnectorSupplier.class).spliterator(), false)
                        .map(s -> s.apply(emailCfg, mxisd))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

                NotificationHandlers.register(() -> new EmailRawNotificationHandler(emailCfg, generators, connectors));
            }
        }

        if (StringUtils.equals(EmailSendGridNotificationHandler.ID, handler)) {
            Object cfgJson = mxisd.getConfig().getNotification().getHandlers().get(EmailSendGridNotificationHandler.ID);
            if (Objects.nonNull(cfgJson)) {
                EmailSendGridConfig cfg;
                try {
                    cfg = GsonUtil.get().fromJson(GsonUtil.get().toJson(cfgJson), EmailSendGridConfig.class);
                } catch (JsonSyntaxException e) {
                    throw new ConfigurationException("Invalid configuration for threepid email sendgrid handler");
                }

                NotificationHandlers.register(() -> new EmailSendGridNotificationHandler(mxisd.getConfig(), cfg));
            }
        }

    }

    private void acceptPhone(String handler, Mxisd mxisd) {
        if (StringUtils.equals(PhoneNotificationHandler.ID, handler)) {
            Object o = mxisd.getConfig().getThreepid().getMedium().get(ThreePidMedium.PhoneNumber.getId());
            if (Objects.nonNull(o)) {
                PhoneConfig cfg;
                try {
                    cfg = GsonUtil.get().fromJson(GsonUtil.makeObj(o), PhoneConfig.class);
                } catch (JsonSyntaxException e) {
                    throw new ConfigurationException("Invalid configuration for threepid msisdn notification");
                }

                List<PhoneGenerator> generators = StreamSupport
                        .stream(ServiceLoader.load(PhoneGeneratorSupplier.class).spliterator(), false)
                        .map(s -> s.apply(cfg, mxisd))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

                List<PhoneConnector> connectors = StreamSupport
                        .stream(ServiceLoader.load(PhoneConnectorSupplier.class).spliterator(), false)
                        .map(s -> s.apply(cfg, mxisd))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

                NotificationHandlers.register(() -> new PhoneNotificationHandler(cfg, generators, connectors));
            }
        }
    }

}
