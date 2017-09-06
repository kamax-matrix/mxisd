package io.kamax.mxisd.exception;

import java.util.Optional;

public class ConfigurationException extends RuntimeException {

    private String key;
    private String detailedMsg;

    public ConfigurationException(String key) {
        super("Invalid or empty value for configuration key " + key);
    }

    public ConfigurationException(Throwable t) {
        super(t.getMessage(), t);
    }

    public ConfigurationException(String key, String detailedMsg) {
        this(key);
        this.detailedMsg = detailedMsg;
    }

    public Optional<String> getDetailedMessage() {
        return Optional.ofNullable(detailedMsg);
    }

}
