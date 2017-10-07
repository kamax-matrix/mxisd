# LAP (Samba / Active Directory / OpenLDAP)
## Getting started
To use your LDAP backend, add the bare minimum configuration in mxisd config file:
```
ldap.enabled: true
ldap.connection.host: 'ldapHostnameOrIp'
ldap.connection.bindDn: 'CN=My Mxisd User,OU=Users,DC=example,DC=org'
ldap.connection.bindPassword: 'TheUserPassword'
ldap.connection.baseDn: 'OU=Users,DC=example,DC=org'
```
These are standard LDAP connection configuration. mxisd will try to connect on port default port 389 without encryption.

---

If you would like to use a TLS/SSL connection, use the following configuration options (STARTLS not supported):
```
ldap.connection.tls: true
ldap.connection.port: 12345
```

---

You can also set a default global filter on any LDAP queries:
```
ldap.filter: '(memberOf=CN=My Matrix Users,OU=Groups,DC=example,DC=org)'
```
This example would only return users part of the group called `My Matrux Users`.
This can be overwritten or append in each specific flow describe below.

---

LDAP features are based on mapping LDAP attributes to Matrix concepts, like a Matrix ID, its localpart, the user display
name, their email(s) and/or phone number(s).
     
Default attributes are well suited for Active Directory/Samba. In case you are using a native LDAP backend, you will
most certainly configure those mappings.

The following example would set the `uid` attribute as localpart and the Matrix display name to `cn`
```
ldap.attribute.uid.type: 'uid'
ldap.attribute.uid.value: 'uid'
ldap.attribute.name: 'cn'
```

You can also change the attribute lists for 3PID, like email or phone numbers.  
The following example would overwrite the [default list of attributes](../../src/main/resources/application.yaml#L67) for emails and phone number:
```
ldap.attribute.threepid.email:
  - 'mail'
  - 'otherMailAttribute'

ldap.attribute.threepid.msisdn:
  - 'phone'
  - 'otherPhoneAttribute'
```

## Identity
Identity features (related to 3PID invites or searches) are enabled and configured using default values and no specific
configuration item is needed to get started.

If you would like to overwrite some global configuration relative to filter and/or attributes, see the Identity section
of the Configuration below.

## Authentication
No further configuration is needed to enable authentication with LDAP once globally enabled and configured.  
You have the possiblity to use a different query filter if you wish, see Configuration below.

## Directory
No further configuration is needed to enable directory with LDAP once globally enabled and configured.

If you would like to use extra attributes in search that are not 3PIDs, like nicknames, group names, employee number:
```
ldap.directory.attribute.other:
  - 'myNicknameAttribute'
  - 'memberOf'
  - 'employeeNumberAttribute'
```

## Configuration
Please read the [Configuration](../configure.md) explanatory note if you are not familiar with the terms used below.
 
### General
Base path: `ldap`

| Item      | Description                                                                               |
|-----------|-------------------------------------------------------------------------------------------|
| `enabled` | Globaly enable/disable the LDAP backend                                                   |
| `filter`  | Global filter to apply on all LDAP queries. Can be overwritten in each applicable section |

### Connection
Base path: `ldap.connection`

| Item           | Description                                          |
|----------------|------------------------------------------------------|
| `host`         | Host to connect to                                   |
| `port`         | Port to use                                          |
| `tls`          | boolean to use TLS or not (STARTLS is not supported) |
| `bindDn`       | Bind DN for authentication                           |
| `bindPassword` | Bind password                                        |
| `baseDn`       | Base DN for queries                                  |

### Attributes
Base path: `ldap.attribute`

| Item        | Description                                                                                                            |
|-------------|------------------------------------------------------------------------------------------------------------------------|
| `uid.type`  | Indicate how to process the User ID (UID) attribute:                                                                   |
|             |   - `uid` will consider the value as the [Localpart](https://matrix.org/docs/spec/intro.html#user-identifiers)         |
|             |   - `mxid` will consider the value as a complete [Matrix ID](https://matrix.org/docs/spec/intro.html#user-identifiers) |
| `uid.value` | Attribute name refering to the User ID. This is typically `userPrincipalName` on AD/Samba setups and `uid` in LDAP     |
| `name`      | Attribute name that contains the [Display Name](https://matrix.org/docs/spec/intro.html#profiles) of the user          |
| `threepid`  | Namespace where each key is a 3PID type and contains a list of attributes                                              |

### Authentication
Base path: `ldap.auth`

| Item     | Description                                                                                      |
|----------|--------------------------------------------------------------------------------------------------|
| `filter` | Specific user filter applied during authentication. Global filter is used if empty/blank/not set |

### Directory
Base path: `ldap.directory`

| Item              | Description                                                         |
|-------------------|---------------------------------------------------------------------|
| `attribute.other` | Additional attributes to be used when performing directory searches |
| `filter`          | Specific user filter applied during directory search.               |
|                   | Global filter is used if empty/blank/not set                        |

### Identity
Base path: `ldap.identity`

| Item     | Description                                                                                       |
|----------|---------------------------------------------------------------------------------------------------|
| `filter` | Specific user filter applied during identity search. Global filter is used if empty/blank/not set | 
| `medium` | Namespace to overwrite generated queries from the list of attributes for each 3PID medium         |
