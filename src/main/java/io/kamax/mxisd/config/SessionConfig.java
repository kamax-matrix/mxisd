/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Kamax Sarl
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

import io.kamax.matrix.json.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

public class SessionConfig {

    private transient final Logger log = LoggerFactory.getLogger(SessionConfig.class);

    public static class Policy {

        public static class PolicyTemplate {

            public static class PolicySource {

                public static class PolicySourceRemote {

                    private boolean enabled;
                    private String server;

                    public boolean isEnabled() {
                        return enabled;
                    }

                    public void setEnabled(boolean enabled) {
                        this.enabled = enabled;
                    }

                    public String getServer() {
                        return server;
                    }

                    public void setServer(String server) {
                        this.server = server;
                    }

                }

                private boolean enabled;
                private boolean toLocal;
                private PolicySourceRemote toRemote = new PolicySourceRemote();

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }

                public boolean toLocal() {
                    return toLocal;
                }

                public void setToLocal(boolean toLocal) {
                    this.toLocal = toLocal;
                }

                public boolean toRemote() {
                    return toRemote.isEnabled();
                }

                public PolicySourceRemote getToRemote() {
                    return toRemote;
                }

                public void setToRemote(PolicySourceRemote toRemote) {
                    this.toRemote = toRemote;
                }

            }

            private boolean enabled;
            private PolicySource forLocal = new PolicySource();
            private PolicySource forRemote = new PolicySource();

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public PolicySource getForLocal() {
                return forLocal;
            }

            public PolicySource forLocal() {
                return forLocal;
            }

            public PolicySource getForRemote() {
                return forRemote;
            }

            public PolicySource forRemote() {
                return forRemote;
            }

            public PolicySource forIf(boolean isLocal) {
                return isLocal ? forLocal : forRemote;
            }

        }

        public Policy() {
            validation.enabled = true;
            validation.forLocal.enabled = true;
            validation.forLocal.toLocal = true;
            validation.forLocal.toRemote.enabled = true;
            validation.forLocal.toRemote.server = "matrix-org";

            validation.forRemote.enabled = true;
            validation.forRemote.toLocal = false;
            validation.forRemote.toRemote.enabled = true;
            validation.forRemote.toRemote.server = "matrix-org";
        }

        private PolicyTemplate validation = new PolicyTemplate();

        public PolicyTemplate getValidation() {
            return validation;
        }

        public void setValidation(PolicyTemplate validation) {
            this.validation = validation;
        }

    }

    private Policy policy = new Policy();

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    @PostConstruct
    public void build() {
        log.info("--- Session config ---");
        log.info("Global Policy: {}", GsonUtil.get().toJson(policy));
    }

}
