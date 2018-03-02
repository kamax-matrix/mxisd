package io.kamax.mxisd.controller.auth.v1.io;

public class AuthenticationDataRequestJson {

    private String type;
    private String user;
    private String medium;
    private String address;

    public AuthenticationDataRequestJson() {
    }

    public AuthenticationDataRequestJson(String type, String user, String medium, String address) {
        this.type = type;
        this.user = user;
        this.medium = medium;
        this.address = address;
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

    @Override
    public String toString() {
        return "AuthenticationDataRequestJson{" +
                "type='" + type + '\'' +
                ", user='" + user + '\'' +
                ", medium='" + medium + '\'' +
                ", address='" + address + '\'' +
                '}';
    }

}
