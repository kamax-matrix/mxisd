package io.kamax.mxisd.exception;

public class ConfigurationException extends RuntimeException {

    private String key;

    public ConfigurationException(String key) {
        super("Invalid or empty value for configuration key " + key);
    }

}
