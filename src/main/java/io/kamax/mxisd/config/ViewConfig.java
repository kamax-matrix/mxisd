/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
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

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@ConfigurationProperties("view")
public class ViewConfig {

    private Logger log = LoggerFactory.getLogger(ViewConfig.class);

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

        public static class Remote {

            private Paths onRequest = new Paths();
            private Paths onCheck = new Paths();

            public Paths getOnRequest() {
                return onRequest;
            }

            public void setOnRequest(Paths onRequest) {
                this.onRequest = onRequest;
            }

            public Paths getOnCheck() {
                return onCheck;
            }

            public void setOnCheck(Paths onCheck) {
                this.onCheck = onCheck;
            }

        }

        private Local local = new Local();
        private Local localRemote = new Local();
        private Remote remote = new Remote();

        public Local getLocal() {
            return local;
        }

        public void setLocal(Local local) {
            this.local = local;
        }

        public Local getLocalRemote() {
            return localRemote;
        }

        public void setLocalRemote(Local localRemote) {
            this.localRemote = localRemote;
        }

        public Remote getRemote() {
            return remote;
        }

        public void setRemote(Remote remote) {
            this.remote = remote;
        }
    }

    private Session session = new Session();

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    @PostConstruct
    public void build() {
        log.info("--- View config ---");
        log.info("Session: {}", new Gson().toJson(session));
    }

}
