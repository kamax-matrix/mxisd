package io.kamax.mxisd.controller.v1.io;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class SessionPhoneTokenRequestJson extends GenericTokenRequestJson {

    private static PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    private String country;
    private String phone_number;

    @Override
    public String getMedium() {
        return "msisdn";
    }

    @Override
    public String getValue() {
        try {
            Phonenumber.PhoneNumber num = phoneUtil.parse(phone_number, country);
            return phoneUtil.format(num, PhoneNumberUtil.PhoneNumberFormat.E164).replace("+", "");
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Invalid phone number");
        }
    }

}
