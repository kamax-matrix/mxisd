# User Directory
This feature allows you to search for existing and/or potential users that are already present in your Identity backend
or that already share a room with you on the Homeserver.

Without any integration, synapse:
- Only search within the users **already** known to you
- Only search on the Display Name and the Matrix ID

With mxisd integration, you can:
- Search on Matrix ID, Display name and 3PIDs (Email, phone numbers) of any users already in your configured backend
- Search for users which you are not in contact with yet. Super useful for corporations who want to give Matrix access
internally, so users can just find themselves **prior** to having any common room(s)
- Use any attribute of your backend to extend the search!

## Overview
This is performed by intercepting the Homeserver endpoint `/_matrix/client/r0/user_directory/search` like so:
```
           +----------------------------------------------+
client --> | Reverse proxy                                                                         Step 2
           |                                                Step 1  +-------------------------+
           |   /_matrix/client/r0/user_directory/search ----------> |                         |  Search in   +---------+
           |                        /\                              | mxisd - Identity server | -----------> | Backend |
           |   /_matrix/*            \----------------------------- |                         |  all users   +---------+
           |        |            Step 4: Send back merged results   +-------------------------+
           +--------|-------                                                     |
                    |                                                          Step 3
                    |                                                            |
                    |    +------------+                                Search in known users
                    \--> | Homeserver | <----------------------------------------/
                         +------------+   /_matrix/client/r0/user_directory/search
```

## Requirements
- Reverse proxy setup, which you should already have in place if you use mxisd
- Compatible backends:
  - LDAP
  - SQL
  - REST
  
## Configuration
### Reverse proxy
Apache2 configuration to put under the relevant virtual domain:
```
ProxyPreserveHost on
ProxyPass /_matrix/identity/ http://mxisdInternalIpAddress:8090/_matrix/identity/
ProxyPass /_matrix/client/r0/user_directory/ http://mxisdInternalIpAddress:8090/_matrix/client/r0/user_directory/
ProxyPass /_matrix/ http://HomeserverInternalIpAddress:8008/_matrix/

```
`ProxyPreserveHost` or equivalent must be enabled to detect to which Homeserver mxisd should talk to when building
results.

### Backend
#### LDAP
Configuration structure has been altered so queries are automatically built from a global or specific filter and a list
of attributes. To ensure Directory feature works, here how the LDAP configuration should look like:
```
ldap:
  enabled: false
  filter: '(memberOf=CN=Matrix Users,OU=Groups,DC=example,DC=org)'
  connection:
    host: 'ldapIpOrDomain'
    bindDn: 'CN=Matrix Identity Server,OU=Accounts,DC=example,DC=org'
    bindPassword: 'mxisd'
    baseDn: 'OU=Accounts,DC=example,DC=org'
  attribute:
    uid:
      type: 'uid'
      value: 'userPrincipalName'
    name: 'displayName'
    threepid:
      email:
        - 'mailPrimaryAddress'
        - 'mail'
        - 'otherMailbox'
      msisdn:
        - 'telephoneNumber'
        - 'mobile'
        - 'homePhone'
        - 'otherTelephone'
        - 'otherMobile'
        - 'otherHomePhone'
  directory:
    attribute:
      other:
        - 'employeeNumber'
        - 'someOtherAttribute'
```
Previous configuration entries that contained queries with the `%3pid` placeholder should not be used anymore, unless
specifically overwritten. Instead, add all attributes to the relevant sections.

If you would like to include an attribute which is not a display name or a 3PID, you can use the
`directory.attribute.other` to list any extra attributes you want included in searches.  
If you do not want to include any extra attribute, that configuration section can be skipped. 

#### SQL
If you plan to integrate directory search directly with synapse, use the `synapseSql` provider, based on the following
config:
```
synapseSql:
  enabled: true
  type: <database ID>
  connection: ``
```
`type` and `connection`, including any other configuration item, follow the same values as the regular `sql` backend.

---

For the regular SQL backend, the following configuration items are available:
```
sql:
  directory:
    enabled: true
    query:
      name:
        type: 'localpart'
        value: 'SELECT idColumn, displayNameColumn FROM table WHERE displayNameColumn LIKE ?'
      threepid:
        type: 'localpart'
        value: 'SELECT idColumn, displayNameColumn FROM table WHERE threepidColumn LIKE ?'
```
For each query, `type` can be used to tell mxisd how to process the ID column:
- `localpart` will append the `matrix.domain` to it
- `mxid` will use the ID as-is. If it is not a valid Matrix ID, the search will fail.

`value` is the SQL query and must return two columns:
- The first being the User ID
- The second being its display name

#### REST
See the [dedicated document](../backends/rest.md)
### DNS Overwrite
Just like you need to configure a reverse proxy to send client requests to mxisd, you also need to configure mxisd with
the internal IP of the Homeserver so it can talk to it directly to integrate its directory search.

To do so, use the following configuration:
```
dns.overwrite.homeserver.client:
  - name: 'example.org'
    value: 'http://localhost:8008'
```
`name` must be the hostname of the URL that clients use when connecting to the Homeserver.  
In case the hostname is the same as your Matrix domain, you can use `${matrix.domain}` to auto-populate the value using
the `matrix.domain` configuration option and avoid duplicating it.

`value` is the base intenral URL of the Homeserver, without any `/_matrix/..` or trailing `/`.
