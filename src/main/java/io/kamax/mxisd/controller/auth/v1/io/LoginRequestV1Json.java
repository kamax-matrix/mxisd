package io.kamax.mxisd.controller.auth.v1.io;

public class LoginRequestV1Json extends ALoginRequest {

    private String user;
    private String medium;
    private String address;

    public LoginRequestV1Json() {
    }

    public LoginRequestV1Json(String type, String password, String token, String device_id, String initial_device_display_name, String user, String medium, String address) {
        super(type, password, token, device_id, initial_device_display_name);
        this.user = user;
        this.medium = medium;
        this.address = address;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String getMedium() {
        return medium;
    }

    @Override
    public void setMedium(String medium) {
        this.medium = medium;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public void removeThirdpartyId() {
        medium = null;
        address = null;
    }

}
