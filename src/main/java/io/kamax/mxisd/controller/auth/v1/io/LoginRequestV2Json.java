package io.kamax.mxisd.controller.auth.v1.io;

public class LoginRequestV2Json extends ALoginRequest {

    public AuthenticationDataRequestJson identifier;

    public LoginRequestV2Json() {
    }

    @Override
    public String getUser() {
        return identifier.getUser();
    }

    @Override
    public void setUser(String user) {
        identifier.setUser(user);
    }

    @Override
    public String getMedium() {
        return identifier.getMedium();
    }

    @Override
    public void setMedium(String medium) {
        identifier.setMedium(medium);
    }

    @Override
    public String getAddress() {
        return identifier.getAddress();
    }

    @Override
    public void setAddress(String address) {
        identifier.setAddress(address);
    }

    @Override
    public void removeThirdpartyId() {
        identifier.setType("m.id.user");
        identifier.setMedium(null);
        identifier.setAddress(null);
        identifier.setNumber(null);
        identifier.setCountry(null);
    }

    public LoginRequestV2Json(String type, String password, String token, String device_id, String initial_device_display_name, AuthenticationDataRequestJson identifier) {
        super(type, password, token, device_id, initial_device_display_name);
        this.identifier = identifier;
    }

    public AuthenticationDataRequestJson getIdentifier() {
        return identifier;
    }

    public void setIdentifier(AuthenticationDataRequestJson identifier) {
        this.identifier = identifier;
    }

}
