package io.kamax.mxisd.controller.v1.io;

public class SessionPhoneTokenRequestJson extends GenericTokenRequestJson {

    private String country;
    private String phone_number;

    @Override
    public String getMedium() {
        return "email";
    }

    @Override
    public String getValue() {
        return phone_number;
    }

    public String getCountry() {
        return country;
    }

}
