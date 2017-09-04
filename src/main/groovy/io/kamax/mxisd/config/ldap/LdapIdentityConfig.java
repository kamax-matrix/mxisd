package io.kamax.mxisd.config.ldap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Configuration
@ConfigurationProperties(prefix = "ldap.identity")
public class LdapIdentityConfig {

    private Map<String, String> medium = new HashMap<>();

    public Map<String, String> getMedium() {
        return medium;
    }

    public Optional<String> getQuery(String key) {
        return Optional.ofNullable(medium.get(key));
    }

    public void setMedium(Map<String, String> medium) {
        this.medium = medium;
    }

}
