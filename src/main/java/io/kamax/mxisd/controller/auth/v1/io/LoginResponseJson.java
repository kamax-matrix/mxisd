package io.kamax.mxisd.controller.auth.v1.io;

public class LoginResponseJson {

    private String user_id;
    private String access_token;
    private String home_server;
    private String device_id;

    public LoginResponseJson() {
    }

    public LoginResponseJson(String user_id, String access_token, String home_server, String device_id) {
        this.user_id = user_id;
        this.access_token = access_token;
        this.home_server = home_server;
        this.device_id = device_id;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getHome_server() {
        return home_server;
    }

    public void setHome_server(String home_server) {
        this.home_server = home_server;
    }

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    @Override
    public String toString() {
        return "LoginResponseJson{" +
                "user_id='" + user_id + '\'' +
                ", access_token='" + access_token + '\'' +
                ", home_server='" + home_server + '\'' +
                ", device_id='" + device_id + '\'' +
                '}';
    }
}
