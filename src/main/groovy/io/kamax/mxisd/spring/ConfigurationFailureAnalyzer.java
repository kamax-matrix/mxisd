package io.kamax.mxisd.spring;

import io.kamax.mxisd.exception.ConfigurationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class ConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<ConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ConfigurationException cause) {
        String message = cause.getMessage();
        if (cause.getDetailedMessage().isPresent()) {
            message += " - " + cause.getDetailedMessage().get();
        }
        return new FailureAnalysis(message, "Double check the key value", cause);
    }

}
