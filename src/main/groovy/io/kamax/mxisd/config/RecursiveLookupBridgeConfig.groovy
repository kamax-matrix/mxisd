package io.kamax.mxisd.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "lookup.recursive.bridge")
class RecursiveLookupBridgeConfig implements InitializingBean {

    private Logger log = LoggerFactory.getLogger(RecursiveLookupBridgeConfig.class)

    private boolean enabled
    private boolean recursiveOnly
    private String server
    private Map<String, String> mappings = new HashMap<>()

    boolean getEnabled() {
        return enabled
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled
    }

    boolean getRecursiveOnly() {
        return recursiveOnly
    }

    void setRecursiveOnly(boolean recursiveOnly) {
        this.recursiveOnly = recursiveOnly
    }

    String getServer() {
        return server
    }

    void setServer(String server) {
        this.server = server
    }

    Map<String, String> getMappings() {
        return mappings
    }

    void setMappings(Map<String, String> mappings) {
        this.mappings = mappings
    }

    @Override
    void afterPropertiesSet() throws Exception {
        log.info("--- Bridge integration lookups config ---")
        log.info("Enabled: {}", getEnabled())
        if (getEnabled()) {
            log.info("Recursive only: {}", getRecursiveOnly())
            log.info("Fallback Server: {}", getServer())
            log.info("Mappings: {}", mappings.size())
        }
    }

}
