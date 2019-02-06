# LDAP Identity store
## Supported products:
- Samba
- Active Directory
- OpenLDAP
- NetIQ eDirectory

For NetIQ, replace all the `ldap` prefix in the configuration by `netiq`.

## Features
|                       Name                      | Supported |
|-------------------------------------------------|-----------|
| [Authentication](../features/authentication.md) | Yes       |
| [Directory](../features/directory.md)           | Yes       |
| [Identity](../features/identity.md)             | Yes       |
| [Profile](../features/profile.md)               | Yes       |

## Getting started
### Base
To use your LDAP backend, add the bare minimum configuration in mxisd config file:
```yaml
ldap:
  enabled: true
  connection:
    host: 'ldapHostnameOrIp'
    port: 389
    bindDn: 'CN=My Mxisd User,OU=Users,DC=example,DC=org'
    bindPassword: 'TheUserPassword'
    baseDNs:
      - 'OU=Users,DC=example,DC=org'
```
These are standard LDAP connection configuration. mxisd will try to connect on port default port 389 without encryption.

If you would like to use several Base DNs, simply add more entries under `baseDNs`.

### TLS/SSL connection
If you would like to use a TLS/SSL connection, use the following configuration options (STARTLS not supported):
```yaml
ldap:
  connection:
    tls: true
    port: 12345
```

### Filter results
You can also set a default global filter on any LDAP queries:
```yaml
ldap:
  filter: '(memberOf=CN=My Matrix Users,OU=Groups,DC=example,DC=org)'
```
This example would only return users part of the group called `My Matrix Users`.
This can be overwritten or append in each specific flow describe below.

For supported syntax, see the [LDAP library documentation](http://directory.apache.org/api/user-guide/2.3-searching.html#filter).

### Attribute mapping
LDAP features are based on mapping LDAP attributes to Matrix concepts, like a Matrix ID, its localpart, the user display
name, their email(s) and/or phone number(s).
     
Default attributes are well suited for Active Directory/Samba. In case you are using a native LDAP backend, you will
most certainly configure those mappings.

#### User ID
`ldap.attribute.uid.type`: How to process the User ID (UID) attribute:
- `uid` will consider the value as the [Localpart](https://matrix.org/docs/spec/intro.html#user-identifiers)
- `mxid` will consider the value as a complete [Matrix ID](https://matrix.org/docs/spec/intro.html#user-identifiers)

`ldap.attribute.uid.value`: Attribute to use to set the User ID value.

The following example would set the `sAMAccountName` attribute as a Matrix User ID localpart:
```yaml
ldap:
  attribute:
    uid:
      type: 'uid'
      value: 'sAMAccountName'
```

#### Display name
Use `ldap.attribute.name`.

The following example would set the display name to the value of the `cn` attribute:
```yaml
ldap:
  attribute:
    name: 'cn'
```

#### 3PIDs
You can also change the attribute lists for 3PID, like email or phone numbers.

The following example would overwrite the [default list of attributes](../../src/main/resources/application.yaml#L67)
for emails and phone number:
```yaml
ldap:
  attribute:
    threepid:
      email:
        - 'mail'
        - 'otherMailAttribute'
      msisdn:
        - 'phone'
        - 'otherPhoneAttribute'
```

## Features
### Identity
Identity features (related to 3PID invites or searches) are enabled and configured using default values and no specific
configuration item is needed to get started.

#### Configuration
- `ldap.identity.filter`: Specific user filter applied during identity search. Global filter is used if blank/not set.
- `ldap.identity.medium`: Namespace to overwrite generated queries from the list of attributes for each 3PID medium.

### Authentication
After you have configured and enabled the [feature itself](../features/authentication.md), no further configuration is
needed with this identity store to make it work.

Profile auto-fill is enabled by default. It will use the `ldap.attribute.name` and `ldap.attribute.threepid` configuration
options to get a lit of attributes to be used to build the user profile to pass on to synapse during authentication.

#### Configuration
- `ldap.auth.filter`: Specific user filter applied during username search. Global filter is used if blank/not set.

### Directory
After you have configured and enabled the [feature itself](../features/directory.md), no further configuration is
needed with this identity store to make it work.

#### Configuration
To set a specific filter applied during directory search, use `ldap.directory.filter`

If you would like to use extra attributes in search that are not 3PIDs, like nicknames, group names, employee number:
```yaml
ldap:
  directory:
    attribute:
      other:
        - 'myNicknameAttribute'
        - 'memberOf'
        - 'employeeNumberAttribute'
```
