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

import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;

@Configuration
@ConfigurationProperties("matrix.listener")
public class ListenerConfig {

    public static class Token {

        private String as;
        private String hs;

        public String getAs() {
            return as;
        }

        public void setAs(String as) {
            this.as = as;
        }

        public String getHs() {
            return hs;
        }

        public void setHs(String hs) {
            this.hs = hs;
        }

    }

    private transient URL csUrl;
    private String url;
    private String localpart;
    private Token token = new Token();

    public URL getUrl() {
        return csUrl;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLocalpart() {
        return localpart;
    }

    public void setLocalpart(String localpart) {
        this.localpart = localpart;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    @PostConstruct
    public void build() throws MalformedURLException {
        if (StringUtils.isBlank(url)) {
            return;
        }

        csUrl = new URL(url);

        if (StringUtils.isBlank(getLocalpart())) {
            throw new IllegalArgumentException("localpart for matrix listener is not set");
        }

        if (StringUtils.isBlank(getToken().getAs())) {
            throw new IllegalArgumentException("AS token is not set");
        }

        if (StringUtils.isBlank(getToken().getHs())) {
            throw new IllegalArgumentException("HS token is not set");
        }
    }

}
