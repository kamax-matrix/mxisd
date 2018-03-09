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

        private Query name = new Query();
        private Query threepid = new Query();

        public Query getName() {
            return name;
        }

        public void setName(Query name) {
            this.name = name;
        }

        public Query getThreepid() {
            return threepid;
        }

        public void setThreepid(Query threepid) {
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
        private Type query = new Type();

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Type getQuery() {
            return query;
        }

        public void setQuery(Type query) {
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

    public static class ProfileThreepids {

        private String query;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

    }

    public static class Profile {

        private ProfileThreepids threepid = new ProfileThreepids();

        public ProfileThreepids getThreepid() {
            return threepid;
        }

        public void setThreepid(ProfileThreepids threepid) {
            this.threepid = threepid;
        }

    }

    private boolean enabled;
    private String type;
    private String connection;
    private Auth auth = new Auth();
    private Directory directory = new Directory();
    private Identity identity = new Identity();
    private Profile profile = new Profile();

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

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Directory getDirectory() {
        return directory;
    }

    public void setDirectory(Directory directory) {
        this.directory = directory;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
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
            log.info("Profile 3PID query: {}", getProfile().getThreepid().getQuery());
        }
    }

}
