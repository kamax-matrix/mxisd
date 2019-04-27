/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2019 Kamax Sarl
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

import io.kamax.matrix.ThreePidMedium;
import io.kamax.matrix.json.GsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class RegisterConfig {

    private static final Logger log = LoggerFactory.getLogger(RegisterConfig.class);

    public static class ThreepidPolicyPattern {

        private List<String> blacklist = new ArrayList<>();
        private List<String> whitelist = new ArrayList<>();

        public List<String> getBlacklist() {
            return blacklist;
        }

        public void setBlacklist(List<String> blacklist) {
            this.blacklist = blacklist;
        }

        public List<String> getWhitelist() {
            return whitelist;
        }

        public void setWhitelist(List<String> whitelist) {
            this.whitelist = whitelist;
        }

    }

    public static class EmailPolicy extends ThreepidPolicy {

        private ThreepidPolicyPattern domain = new ThreepidPolicyPattern();

        public ThreepidPolicyPattern getDomain() {
            return domain;
        }

        public void setDomain(ThreepidPolicyPattern domain) {
            this.domain = domain;
        }

        private List<String> buildPatterns(List<String> domains) {
            log.debug("Building email policy");
            return domains.stream().map(d -> {
                if (StringUtils.startsWith(d, "*")) {
                    log.debug("Found domain and subdomain policy");
                    d = "(.*)" + d.substring(1);
                } else if (StringUtils.startsWith(d, ".")) {
                    log.debug("Found subdomain-only policy");
                    d = "(.*)" + d;
                } else {
                    log.debug("Found domain-only policy");
                }

                return "([^@]+)@" + d.replace(".", "\\.");
            }).collect(Collectors.toList());
        }

        @Override
        public void build() {
            if (Objects.isNull(getDomain())) {
                return;
            }

            if (Objects.nonNull(getDomain().getBlacklist())) {
                if (Objects.isNull(getPattern().getBlacklist())) {
                    getPattern().setBlacklist(new ArrayList<>());
                }

                List<String> domains = buildPatterns(getDomain().getBlacklist());
                getPattern().getBlacklist().addAll(domains);
            }

            if (Objects.nonNull(getDomain().getWhitelist())) {
                if (Objects.isNull(getPattern().getWhitelist())) {
                    getPattern().setWhitelist(new ArrayList<>());
                }

                List<String> domains = buildPatterns(getDomain().getWhitelist());
                getPattern().getWhitelist().addAll(domains);
            }

            setDomain(null);
        }

    }

    public static class ThreepidPolicy {

        private ThreepidPolicyPattern pattern = new ThreepidPolicyPattern();

        public ThreepidPolicyPattern getPattern() {
            return pattern;
        }

        public void setPattern(ThreepidPolicyPattern pattern) {
            this.pattern = pattern;
        }

        public void build() {
            // no-op
        }

    }

    public static class Policy {

        private boolean allowed;
        private boolean invite = true;
        private Map<String, Object> threepid = new HashMap<>();

        public boolean isAllowed() {
            return allowed;
        }

        public void setAllowed(boolean allowed) {
            this.allowed = allowed;
        }

        public boolean forInvite() {
            return invite;
        }

        public void setInvite(boolean invite) {
            this.invite = invite;
        }

        public Map<String, Object> getThreepid() {
            return threepid;
        }

        public void setThreepid(Map<String, Object> threepid) {
            this.threepid = threepid;
        }

    }

    private Policy policy = new Policy();

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public void build() {
        log.debug("--- Registration config ---");

        log.debug("Before Build");
        log.debug(GsonUtil.getPrettyForLog(this));

        new HashMap<>(getPolicy().getThreepid()).forEach((medium, policy) -> {
            if (ThreePidMedium.Email.is(medium)) {
                EmailPolicy pPolicy = GsonUtil.get().fromJson(GsonUtil.get().toJson(policy), EmailPolicy.class);
                pPolicy.build();
                policy = GsonUtil.makeObj(pPolicy);
            } else {
                ThreepidPolicy pPolicy = GsonUtil.get().fromJson(GsonUtil.get().toJson(policy), ThreepidPolicy.class);
                pPolicy.build();
                policy = GsonUtil.makeObj(pPolicy);
            }

            getPolicy().getThreepid().put(medium, policy);
        });

        log.debug("After Build");
        log.debug(GsonUtil.getPrettyForLog(this));
    }

}
