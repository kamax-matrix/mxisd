package io.kamax.mxisd.mapping;

public interface MappingSession {

    String getServer();

    String getSecret();

    int getAttempt();

    String getMedium();

    String getValue();

}
