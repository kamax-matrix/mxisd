package io.kamax.mxisd.config.ldap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ldap.attribute")
public class LdapAttributeConfig {

    private LdapAttributeUidConfig uid;
    private String name;

    public LdapAttributeUidConfig getUid() {
        return uid;
    }

    public void setUid(LdapAttributeUidConfig uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
