package io.kamax.mxisd.config.invite.sender;

import io.kamax.mxisd.exception.ConfigurationException;
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
        if (StringUtils.isBlank(getContentPath())) {
            throw new ConfigurationException("invite.sender.email.contentPath");
        }

        File cp = new File(getContentPath()).getAbsoluteFile();
        if (!cp.exists() || !cp.isFile() || !cp.canRead()) {
            throw new ConfigurationException("invite.sender.email.contentPath", getContentPath() + " does not exist, is not a file or cannot be read");
        }

        log.info("--- E-mail Invite Sender config ---");
        log.info("Host: {}", getHost());
        log.info("Port: {}", getPort());
        log.info("TLS Mode: {}", getTls());
        log.info("Login: {}", getLogin());
        log.info("Has password: {}", StringUtils.isBlank(getPassword()));
        log.info("E-mail: {}", getEmail());
        log.info("Content path: {}", cp.getAbsolutePath());
    }

}
