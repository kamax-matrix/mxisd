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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewConfig {

    private static final Logger log = LoggerFactory.getLogger(ViewConfig.class);

    public static class Session {

        public static class Paths {

            private String failure;
            private String success;

            public String getFailure() {
                return failure;
            }

            public void setFailure(String failure) {
                this.failure = failure;
            }

            public String getSuccess() {
                return success;
            }

            public void setSuccess(String success) {
                this.success = success;
            }

        }

        public static class Local {

            private Paths onTokenSubmit = new Paths();

            public Paths getOnTokenSubmit() {
                return onTokenSubmit;
            }

            public void setOnTokenSubmit(Paths onTokenSubmit) {
                this.onTokenSubmit = onTokenSubmit;
            }

        }

        // Legacy option
        private Local local = new Local();
        private Paths onTokenSubmit = new Paths();

        public Session() {
            onTokenSubmit.success = "classpath:/templates/session/tokenSubmitSuccess.html";
            onTokenSubmit.failure = "classpath:/templates/session/tokenSubmitFailure.html";
        }

        public Local getLocal() {
            return local;
        }

        public void setLocal(Local local) {
            this.local = local;
        }

        public Paths getOnTokenSubmit() {
            return onTokenSubmit;
        }

        public void setOnTokenSubmit(Paths onTokenSubmit) {
            this.onTokenSubmit = onTokenSubmit;
        }

    }

    private Session session = new Session();

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void build() {
        if (StringUtils.isNotBlank(session.local.onTokenSubmit.success) && StringUtils.isBlank(session.onTokenSubmit.success)) {
            log.warn("Legacy option session.local.onTokenSubmit.success in use, please switch to session.onTokenSubmit.success");
            session.onTokenSubmit.success = session.local.onTokenSubmit.success;
        }

        if (StringUtils.isNotBlank(session.local.onTokenSubmit.failure) && StringUtils.isBlank(session.onTokenSubmit.failure)) {
            log.warn("Legacy option session.local.onTokenSubmit.failure in use, please switch to session.onTokenSubmit.failure");
            session.onTokenSubmit.failure = session.local.onTokenSubmit.failure;
        }


        log.info("--- View config ---");
        log.info("Session: {}", GsonUtil.get().toJson(session));
    }

}
