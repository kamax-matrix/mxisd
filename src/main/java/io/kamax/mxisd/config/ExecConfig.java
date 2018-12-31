/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxisd.config;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class ExecConfig {

    public class IO {

        private String type;
        private String template;

        public Optional<String> getType() {
            return Optional.ofNullable(type);
        }

        public void setType(String type) {
            this.type = type;
        }

        public Optional<String> getTemplate() {
            return Optional.ofNullable(template);
        }

        public void setTemplate(String template) {
            this.template = template;
        }

    }

    public class Exit {

        private List<Integer> success = Collections.singletonList(0);
        private List<Integer> failure = Collections.singletonList(1);

        public List<Integer> getSuccess() {
            return success;
        }

        public void setSuccess(List<Integer> success) {
            this.success = success;
        }

        public List<Integer> getFailure() {
            return failure;
        }

        public void setFailure(List<Integer> failure) {
            this.failure = failure;
        }

    }

    public class TokenOverride {

        private String localpart;
        private String domain;
        private String mxid;
        private String password;
        private String medium;
        private String address;
        private String type;
        private String query;

        public String getLocalpart() {
            return StringUtils.defaultIfEmpty(localpart, getToken().getLocalpart());
        }

        public void setLocalpart(String localpart) {
            this.localpart = localpart;
        }

        public String getDomain() {
            return StringUtils.defaultIfEmpty(domain, getToken().getDomain());
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getMxid() {
            return StringUtils.defaultIfEmpty(mxid, getToken().getMxid());
        }

        public void setMxid(String mxid) {
            this.mxid = mxid;
        }

        public String getPassword() {
            return StringUtils.defaultIfEmpty(password, getToken().getPassword());
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getMedium() {
            return StringUtils.defaultIfEmpty(medium, getToken().getMedium());
        }

        public void setMedium(String medium) {
            this.medium = medium;
        }

        public String getAddress() {
            return StringUtils.defaultIfEmpty(address, getToken().getAddress());
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getType() {
            return StringUtils.defaultIfEmpty(type, getToken().getType());
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getQuery() {
            return StringUtils.defaultIfEmpty(query, getToken().getQuery());
        }

        public void setQuery(String query) {
            this.query = query;
        }

    }

    public class Token {

        private String localpart = "{localpart}";
        private String domain = "{domain}";
        private String mxid = "{mxid}";
        private String password = "{password}";
        private String medium = "{medium}";
        private String address = "{address}";
        private String type = "{type}";
        private String query = "{query}";

        public String getLocalpart() {
            return localpart;
        }

        public void setLocalpart(String localpart) {
            this.localpart = localpart;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getMxid() {
            return mxid;
        }

        public void setMxid(String mxid) {
            this.mxid = mxid;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getMedium() {
            return medium;
        }

        public void setMedium(String medium) {
            this.medium = medium;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
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

    }

    public class Process {

        private TokenOverride token = new TokenOverride();
        private String command;

        private List<String> args = new ArrayList<>();
        private Map<String, String> env = new HashMap<>();
        private IO input = new IO();

        private Exit exit = new Exit();
        private IO output = new IO();

        public TokenOverride getToken() {
            return token;
        }

        public void setToken(TokenOverride token) {
            this.token = token;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public List<String> getArgs() {
            return args;
        }

        public void setArgs(List<String> args) {
            this.args = args;
        }

        public Map<String, String> getEnv() {
            return env;
        }

        public void setEnv(Map<String, String> env) {
            this.env = env;
        }

        public void addEnv(String key, String value) {
            this.env.put(key, value);
        }

        public IO getInput() {
            return input;
        }

        public void setInput(IO input) {
            this.input = input;
        }

        public Exit getExit() {
            return exit;
        }

        public void setExit(Exit exit) {
            this.exit = exit;
        }

        public IO getOutput() {
            return output;
        }

        public void setOutput(IO output) {
            this.output = output;
        }

    }

    public class Auth extends Process {

        private Boolean enabled;

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

    }

    public class Directory {

        public class Search {

            private Process byName = new Process();
            private Process byThreepid = new Process();

            public Process getByName() {
                return byName;
            }

            public void setByName(Process byName) {
                this.byName = byName;
            }

            public Process getByThreepid() {
                return byThreepid;
            }

            public void setByThreepid(Process byThreepid) {
                this.byThreepid = byThreepid;
            }

        }

        private Boolean enabled;
        private Search search = new Search();

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Search getSearch() {
            return search;
        }

        public void setSearch(Search search) {
            this.search = search;
        }

    }

    public class Lookup {

        private Process single = new Process();
        private Process bulk = new Process();

        public Process getSingle() {
            return single;
        }

        public void setSingle(Process single) {
            this.single = single;
        }

        public Process getBulk() {
            return bulk;
        }

        public void setBulk(Process bulk) {
            this.bulk = bulk;
        }

    }

    public class Identity {

        private Boolean enabled;
        private int priority;
        private Lookup lookup = new Lookup();

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public Lookup getLookup() {
            return lookup;
        }

        public void setLookup(Lookup lookup) {
            this.lookup = lookup;
        }

    }

    public class Profile {

        private Boolean enabled;
        private Process displayName = new Process();
        private Process threePid = new Process();
        private Process role = new Process();

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Process getDisplayName() {
            return displayName;
        }

        public void setDisplayName(Process displayName) {
            this.displayName = displayName;
        }

        public Process getThreePid() {
            return threePid;
        }

        public void setThreePid(Process threePid) {
            this.threePid = threePid;
        }

        public Process getRole() {
            return role;
        }

        public void setRoles(Process role) {
            this.role = role;
        }

    }

    private boolean enabled;
    private Token token = new Token();
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

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
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

    public ExecConfig build() {
        if (Objects.isNull(getAuth().isEnabled())) {
            getAuth().setEnabled(isEnabled());
        }

        if (Objects.isNull(getDirectory().isEnabled())) {
            getDirectory().setEnabled(isEnabled());
        }

        if (Objects.isNull(getIdentity().isEnabled())) {
            getIdentity().setEnabled(isEnabled());
        }

        if (Objects.isNull(getProfile().isEnabled())) {
            getProfile().setEnabled(isEnabled());
        }

        return this;
    }

}
