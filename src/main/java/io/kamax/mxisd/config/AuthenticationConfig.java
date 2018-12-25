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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AuthenticationConfig {

    public static class Rule {

        private String regex;
        private transient Pattern pattern;
        private String medium;

        public String getRegex() {
            return regex;
        }

        public void setRegex(String regex) {
            this.regex = regex;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public void setPattern(Pattern pattern) {
            this.pattern = pattern;
        }

        public String getMedium() {
            return medium;
        }

        public void setMedium(String medium) {
            this.medium = medium;
        }

    }

    public static class User {

        private List<Rule> rules = new ArrayList<>();

        public List<Rule> getRules() {
            return rules;
        }

        public void setRules(List<Rule> mappings) {
            this.rules = mappings;
        }

    }

    public static class Rewrite {

        private User user = new User();

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

    }

    private Rewrite rewrite = new Rewrite();

    public Rewrite getRewrite() {
        return rewrite;
    }

    public void setRewrite(Rewrite rewrite) {
        this.rewrite = rewrite;
    }

    public void build() {
        getRewrite().getUser().getRules().forEach(mapping -> mapping.setPattern(Pattern.compile(mapping.getRegex())));
    }

}
