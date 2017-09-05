package io.kamax.mxisd.config.ldap;

import io.kamax.mxisd.lookup.provider.LdapProvider;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@ConfigurationProperties(prefix = "ldap.attribute.uid")
public class LdapAttributeUidConfig {

    private String type;
    private String value;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @PostConstruct
    public void postConstruct() {
        if (!StringUtils.equals(LdapProvider.UID, getType()) && !StringUtils.equals(LdapProvider.MATRIX_ID, getType())) {
            throw new IllegalArgumentException("Unsupported LDAP UID type: " + getType());
        }
    }

}
