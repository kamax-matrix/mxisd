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

package io.kamax.mxisd.http.io.identity;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class SessionPhoneTokenRequestJson extends GenericTokenRequestJson {

    private static PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    private String country;
    private String phone_number;

    public String getMedium() {
        return "msisdn";
    }

    public String getValue() {
        try {
            Phonenumber.PhoneNumber num = phoneUtil.parse(phone_number, country);
            return phoneUtil.format(num, PhoneNumberUtil.PhoneNumberFormat.E164).replace("+", "");
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Invalid phone number");
        }
    }

}
