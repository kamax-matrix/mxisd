package io.kamax.mxisd.controller.v1.io;

import io.kamax.mxisd.mapping.MappingSession;

public abstract class GenericTokenRequestJson implements MappingSession {

    private String client_secret;
    private int send_attempt;
    private String id_server;

    public String getSecret() {
        return client_secret;
    }

    public int getAttempt() {
        return send_attempt;
    }

    public String getServer() {
        return id_server;
    }

}
