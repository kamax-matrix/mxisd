package io.kamax.mxisd.lookup

import io.kamax.mxisd.api.ThreePidType
import io.kamax.mxisd.config.LdapConfig
import org.apache.directory.api.ldap.model.cursor.EntryCursor
import org.apache.directory.api.ldap.model.entry.Attribute
import org.apache.directory.api.ldap.model.message.SearchScope
import org.apache.directory.ldap.client.api.LdapConnection
import org.apache.directory.ldap.client.api.LdapNetworkConnection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class LdapProvider implements ThreePidProvider {

    @Autowired
    private LdapConfig ldapCfg

    @Override
    int getPriority() {
        return 20
    }

    @Override
    Optional<?> find(ThreePidType type, String threePid) {
        LdapConnection conn = new LdapNetworkConnection(ldapCfg.getHost(), ldapCfg.getPort())
        try {
            conn.bind(ldapCfg.getBindDn(), ldapCfg.getBindPassword())

            String searchQuery = ldapCfg.getQuery().replaceAll("%3pid", threePid)
            EntryCursor cursor = conn.search(ldapCfg.getBaseDn(), searchQuery, SearchScope.SUBTREE, ldapCfg.getAttribute())
            try {
                if (cursor.next()) {
                    Attribute attribute = cursor.get().get(ldapCfg.getAttribute())
                    if (attribute != null) {
                        return Optional.of([
                                address   : threePid,
                                medium    : type,
                                mxid      : attribute.get().toString(),
                                not_before: 0,
                                not_after : 9223372036854775807,
                                ts        : 0
                        ])
                    }
                }
            } finally {
                cursor.close()
            }
        } finally {
            conn.close()
        }

        return Optional.empty()
    }

}
