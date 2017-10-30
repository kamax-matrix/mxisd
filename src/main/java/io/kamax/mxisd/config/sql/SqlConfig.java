package io.kamax.mxisd.config.sql;

import io.kamax.mxisd.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

public abstract class SqlConfig {

    private Logger log = LoggerFactory.getLogger(SqlConfig.class);

    public static class Query {

        private String type;
        private String value;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

    public static class Type {

        private GenericSqlProviderConfig.Query name = new GenericSqlProviderConfig.Query();
        private GenericSqlProviderConfig.Query threepid = new GenericSqlProviderConfig.Query();

        public GenericSqlProviderConfig.Query getName() {
            return name;
        }

        public void setName(GenericSqlProviderConfig.Query name) {
            this.name = name;
        }

        public GenericSqlProviderConfig.Query getThreepid() {
            return threepid;
        }

        public void setThreepid(GenericSqlProviderConfig.Query threepid) {
            this.threepid = threepid;
        }

    }

    public static class Auth {

        private Boolean enabled;

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

    }

    public static class Directory {

        private Boolean enabled;
        private GenericSqlProviderConfig.Type query = new GenericSqlProviderConfig.Type();

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public GenericSqlProviderConfig.Type getQuery() {
            return query;
        }

        public void setQuery(GenericSqlProviderConfig.Type query) {
            this.query = query;
        }

    }

    public static class Identity {

        private Boolean enabled;
        private String type;
        private String query;
        private Map<String, String> medium = new HashMap<>();

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public Map<String, String> getMedium() {
            return medium;
        }

        public void setMedium(Map<String, String> medium) {
            this.medium = medium;
        }

    }

    private boolean enabled;
    private String type;
    private String connection;
    private GenericSqlProviderConfig.Auth auth = new GenericSqlProviderConfig.Auth();
    private GenericSqlProviderConfig.Directory directory = new GenericSqlProviderConfig.Directory();
    private GenericSqlProviderConfig.Identity identity = new GenericSqlProviderConfig.Identity();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public GenericSqlProviderConfig.Auth getAuth() {
        return auth;
    }

    public void setAuth(GenericSqlProviderConfig.Auth auth) {
        this.auth = auth;
    }

    public GenericSqlProviderConfig.Directory getDirectory() {
        return directory;
    }

    public void setDirectory(GenericSqlProviderConfig.Directory directory) {
        this.directory = directory;
    }

    public GenericSqlProviderConfig.Identity getIdentity() {
        return identity;
    }

    public void setIdentity(GenericSqlProviderConfig.Identity identity) {
        this.identity = identity;
    }

    protected abstract String getProviderName();

    protected void doBuild() {
        if (getAuth().isEnabled() == null) {
            getAuth().setEnabled(isEnabled());
        }

        if (getDirectory().isEnabled() == null) {
            getDirectory().setEnabled(isEnabled());
        }

        if (getIdentity().isEnabled() == null) {
            getIdentity().setEnabled(isEnabled());
        }
    }

    @PostConstruct
    public void build() {
        log.info("--- " + getProviderName() + " Provider config ---");

        doBuild();

        log.info("Enabled: {}", isEnabled());
        if (isEnabled()) {
            log.info("Type: {}", getType());
            log.info("Connection: {}", getConnection());
            log.info("Auth enabled: {}", getAuth().isEnabled());
            log.info("Directory queries: {}", GsonUtil.build().toJson(getDirectory().getQuery()));
            log.info("Identity type: {}", getIdentity().getType());
            log.info("3PID mapping query: {}", getIdentity().getQuery());
            log.info("Identity medium queries: {}", GsonUtil.build().toJson(getIdentity().getMedium()));
        }
    }

}
