package io.kamax.mxisd.controller.auth.v1.io;

public abstract class ALoginRequest {

    private String type;
    private String password;
    private String token;
    private String device_id;
    private String initial_device_display_name;

    public ALoginRequest() {
    }

    public ALoginRequest(String type, String password, String token, String device_id, String initial_device_display_name) {
        this.type = type;
        this.password = password;
        this.token = token;
        this.device_id = device_id;
        this.initial_device_display_name = initial_device_display_name;
    }

    public abstract String getUser();

    public abstract void setUser(String user);

    public abstract String getMedium();

    public abstract void setMedium(String medium);

    public abstract String getAddress();

    public abstract void setAddress(String address);

    public abstract void removeThirdpartyId();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getInitial_device_display_name() {
        return initial_device_display_name;
    }

    public void setInitial_device_display_name(String initial_device_display_name) {
        this.initial_device_display_name = initial_device_display_name;
    }
}
