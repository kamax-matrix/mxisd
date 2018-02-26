package io.kamax.mxisd.controller.auth.v1.io;

public class LoginRequestJson {

    private String type;
    private String user;
    private String medium;
    private String address;
    private String password;
    private String token;
    private String device_id;
    private String initial_device_display_name;

    public LoginRequestJson() {
    }

    public LoginRequestJson(String type, String user, String medium, String address, String password, String token, String device_id, String initial_device_display_name) {
        this.type = type;
        this.user = user;
        this.medium = medium;
        this.address = address;
        this.password = password;
        this.token = token;
        this.device_id = device_id;
        this.initial_device_display_name = initial_device_display_name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    @Override
    public String toString() {
        return "LoginRequestJson{" +
                "type='" + type + '\'' +
                ", user='" + user + '\'' +
                ", medium='" + medium + '\'' +
                ", address='" + address + '\'' +
                ", password='" + password + '\'' +
                ", token='" + token + '\'' +
                ", device_id='" + device_id + '\'' +
                ", initial_device_display_name='" + initial_device_display_name + '\'' +
                '}';
    }
}
