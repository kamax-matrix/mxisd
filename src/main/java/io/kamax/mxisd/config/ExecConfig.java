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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.*;

@Configuration
@ConfigurationProperties("exec")
public class ExecConfig {

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

    }

    public class Token {

        private String localpart = "{localpart}";
        private String domain = "{domain}";
        private String mxid = "{mxid}";
        private String password = "{password}";

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

    }

    public class Process {

        private TokenOverride token = new TokenOverride();
        private String command;

        private List<String> args = new ArrayList<>();
        private Map<String, String> env = new HashMap<>();
        private String input;

        private Exit exit = new Exit();
        private String output;

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

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }

        public Exit getExit() {
            return exit;
        }

        public void setExit(Exit exit) {
            this.exit = exit;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
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

    public class Directory extends Process {

        private Boolean enabled;

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

    }

    public class Identity extends Process {

        private Boolean enabled;

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

    }

    public class Profile extends Process {

        private Boolean enabled;

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
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

    @PostConstruct
    public void build() {
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
    }

}
