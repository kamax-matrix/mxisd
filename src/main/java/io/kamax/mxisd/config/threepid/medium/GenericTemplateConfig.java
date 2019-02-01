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

package io.kamax.mxisd.config.threepid.medium;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class GenericTemplateConfig {

    private static final String classpathPrefix = "classpath:";

    protected static String getName(String path) {
        if (StringUtils.startsWith(path, classpathPrefix)) {
            return "Built-in (" + path.substring(classpathPrefix.length()) + ")";
        }

        return path;
    }

    public static class Session {

        public static class SessionValidation {

            private String local;
            private String remote;

            public String getLocal() {
                return local;
            }

            public void setLocal(String local) {
                this.local = local;
            }

            public String getRemote() {
                return remote;
            }

            public void setRemote(String remote) {
                this.remote = remote;
            }

        }

        public static class SessionUnbind {

            private String fraudulent;

            public String getFraudulent() {
                return fraudulent;
            }

            public void setFraudulent(String fraudulent) {
                this.fraudulent = fraudulent;
            }

        }

        private SessionValidation validation = new SessionValidation();
        private SessionUnbind unbind = new SessionUnbind();

        public SessionValidation getValidation() {
            return validation;
        }

        public void setValidation(SessionValidation validation) {
            this.validation = validation;
        }

        public SessionUnbind getUnbind() {
            return unbind;
        }

        public void setUnbind(SessionUnbind unbind) {
            this.unbind = unbind;
        }

    }

    private String invite;
    private Session session = new Session();
    private Map<String, String> generic = new HashMap<>();

    public String getInvite() {
        return invite;
    }

    public void setInvite(String invite) {
        this.invite = invite;
    }

    public Session getSession() {
        return session;
    }

    public Map<String, String> getGeneric() {
        return generic;
    }

    public void setGeneric(Map<String, String> generic) {
        this.generic = generic;
    }

}
