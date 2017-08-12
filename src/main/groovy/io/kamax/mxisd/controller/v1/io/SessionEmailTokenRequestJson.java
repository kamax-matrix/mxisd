package io.kamax.mxisd.controller.v1.io;

public class SessionEmailTokenRequestJson extends GenericTokenRequestJson {

    private String email;

    @Override
    public String getMedium() {
        return "email";
    }

    @Override
    public String getValue() {
        return email;
    }

}
