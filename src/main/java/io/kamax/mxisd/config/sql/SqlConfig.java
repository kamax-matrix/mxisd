package io.kamax.mxisd.config.sql;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        private SqlProviderConfig.Query name = new SqlProviderConfig.Query();
        private SqlProviderConfig.Query threepid = new SqlProviderConfig.Query();

        public SqlProviderConfig.Query getName() {
            return name;
        }

        public void setName(SqlProviderConfig.Query name) {
            this.name = name;
        }

        public SqlProviderConfig.Query getThreepid() {
            return threepid;
        }

        public void setThreepid(SqlProviderConfig.Query threepid) {
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
        private SqlProviderConfig.Type query = new SqlProviderConfig.Type();

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public SqlProviderConfig.Type getQuery() {
            return query;
        }

        public void setQuery(SqlProviderConfig.Type query) {
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
    private SqlProviderConfig.Auth auth = new SqlProviderConfig.Auth();
    private SqlProviderConfig.Directory directory = new SqlProviderConfig.Directory();
    private SqlProviderConfig.Identity identity = new SqlProviderConfig.Identity();

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

    public SqlProviderConfig.Auth getAuth() {
        return auth;
    }

    public void setAuth(SqlProviderConfig.Auth auth) {
        this.auth = auth;
    }

    public SqlProviderConfig.Directory getDirectory() {
        return directory;
    }

    public void setDirectory(SqlProviderConfig.Directory directory) {
        this.directory = directory;
    }

    public SqlProviderConfig.Identity getIdentity() {
        return identity;
    }

    public void setIdentity(SqlProviderConfig.Identity identity) {
        this.identity = identity;
    }

    protected abstract String getProviderName();

    public void build() {
        log.info("--- " + getProviderName() + " Provider config ---");

        if (getAuth().isEnabled() == null) {
            getAuth().setEnabled(isEnabled());
        }

        if (getDirectory().isEnabled() == null) {
            getDirectory().setEnabled(isEnabled());
        }

        if (getIdentity().isEnabled() == null) {
            getIdentity().setEnabled(isEnabled());
        }

        log.info("Enabled: {}", isEnabled());
        if (isEnabled()) {
            log.info("Type: {}", getType());
            log.info("Connection: {}", getConnection());
            log.info("Auth enabled: {}", getAuth().isEnabled());
            log.info("Identity type: {}", getIdentity().getType());
            log.info("Identity medium queries: {}", new Gson().toJson(getIdentity().getMedium()));
        }
    }

}
