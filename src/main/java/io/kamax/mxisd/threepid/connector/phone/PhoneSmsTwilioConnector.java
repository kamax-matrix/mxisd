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

package io.kamax.mxisd.threepid.connector.phone;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.kamax.mxisd.config.threepid.connector.PhoneTwilioConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PhoneSmsTwilioConnector implements IPhoneConnector {

    private Logger log = LoggerFactory.getLogger(PhoneSmsTwilioConnector.class);

    private PhoneTwilioConfig cfg;

    @Autowired
    public PhoneSmsTwilioConnector(PhoneTwilioConfig cfg) {
        this.cfg = cfg;

        Twilio.init(cfg.getAccountSid(), cfg.getAuthToken());
        log.info("Twilio API has been initiated");
    }

    @Override
    public String getId() {
        return "twilio";
    }

    @Override
    public void send(String recipient, String content) {
        recipient = "+" + recipient;
        log.info("Sending SMS notification from {} to {} with {} characters", cfg.getNumber(), recipient, content.length());
        Message.creator(new PhoneNumber("+" + recipient), new PhoneNumber(cfg.getNumber()), content).create();
    }

}
