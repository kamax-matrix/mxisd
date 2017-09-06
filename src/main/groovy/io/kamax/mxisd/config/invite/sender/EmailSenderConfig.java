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

package io.kamax.mxisd.config.invite.sender;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration
@ConfigurationProperties(prefix = "invite.sender.email")
public class EmailSenderConfig {

    private Logger log = LoggerFactory.getLogger(EmailSenderConfig.class);

    private String host;
    private int port;
    private int tls;
    private String login;
    private String password;
    private String email;
    private String name;
    private String contentPath;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTls() {
        return tls;
    }

    public void setTls(int tls) {
        this.tls = tls;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    @PostConstruct
    private void postConstruct() {
        log.info("--- E-mail Invite Sender config ---");
        log.info("Host: {}", getHost());
        log.info("Port: {}", getPort());
        log.info("TLS Mode: {}", getTls());
        log.info("Login: {}", getLogin());
        log.info("Has password: {}", StringUtils.isBlank(getPassword()));
        log.info("E-mail: {}", getEmail());
        if (StringUtils.isBlank(getContentPath())) {
            log.warn("invite.sender.contentPath is empty! Will not send invites");
        } else {
            File cp = new File(getContentPath()).getAbsoluteFile();
            log.info("Content path: {}", cp.getAbsolutePath());
            if (!cp.exists() || !cp.isFile() || !cp.canRead()) {
                log.warn(getContentPath() + " does not exist, is not a file or cannot be read");
            }
        }
    }

}
