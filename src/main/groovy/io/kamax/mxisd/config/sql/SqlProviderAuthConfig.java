package io.kamax.mxisd.config.sql;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Unused
@Configuration
@ConfigurationProperties("sql.auth")
public class SqlProviderAuthConfig {

    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
