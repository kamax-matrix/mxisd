package io.kamax.mxisd.spring;

import io.kamax.mxisd.exception.ConfigurationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class ConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<ConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ConfigurationException cause) {
        return new FailureAnalysis(cause.getMessage(), "Double check the key value", cause);
    }

}
