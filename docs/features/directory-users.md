# User Directory
- [Description](#description)
- [Overview](#overview)
- [Requirements](#requirements)
- [Configuration](#configuration)
  - [Reverse Proxy](#reverse-proxy)
    - [Apache2](#apache2)
    - [nginx](#nginx)
  - [DNS Overwrite](#dns-overwrite)
  - [Backends](#backends)
    - [LDAP](#ldap)
    - [SQL](#sql)
    - [REST](#rest)
- [Next steps](#next-steps)

## Description
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
- Include your homeserver search results to those found by mxisd (default behaviour, no configuration required)

By integrating mxisd, you get the default behaviour with all the extras, ensuring your users will always find each other.

## Overview
This is performed by intercepting the Homeserver endpoint `/_matrix/client/r0/user_directory/search` like so:
```
           +----------------------------------------------+
Client --> | Reverse proxy                                                                         Step 2
           |                                              Step 1    +-------------------------+
           |   /_matrix/client/r0/user_directory/search ----------> |                         |  Search in   +---------+
           |                        /\                              | mxisd - Identity server | -----------> | Backend |
           |   /_matrix/*            \----------------------------- |                         |  all users   +---------+
           |        |            Step 4: Send back merged results   +-------------------------+
           +        |                                                            |
                    |                                                          Step 3
                    |                                                            |
                    |    +------------+                                Search in known users
                    \--> | Homeserver | <----------------------------------------/
                         +------------+   /_matrix/client/r0/user_directory/search
```
Steps:
1. The intercepted request is directly sent to mxisd instead of the Homeserver.
2. Enabled backends are queried for any match on the search value sent by the client.
3. The Homeserver, from which the request was intercepted, is queried using the same request as the client.
Its address is resolved using the DNS Overwrite feature to reach its internal address on a non-encrypted port.
4. Results from backends and the Homeserver are merged together and sent back to the client, believing it was the HS
which directly answered the request.

## Requirements
- Reverse proxy setup, which you should already have in place if you use mxisd
- Compatible backends:
  - LDAP
  - SQL
  - REST
  
## Configuration
### Reverse Proxy
#### Apache2
The specific configuration to put under the relevant `VirtualHost`:
```
ProxyPass /_matrix/client/r0/user_directory/ http://0.0.0.0:8090/_matrix/client/r0/user_directory/
```
`ProxyPreserveHost` or equivalent must be enabled to detect to which Homeserver mxisd should talk to when building
results.

Your `VirtualHost` should now look like this:
```
<VirtualHost *:443>
    ServerName example.org
    
    ...
    
    ProxyPreserveHost on
    ProxyPass /_matrix/client/r0/user_directory/ http://localhost:8090/_matrix/client/r0/user_directory/
    ProxyPass /_matrix/identity/ http://localhost:8090/_matrix/identity/
    ProxyPass /_matrix/ http://localhost:8008/_matrix/
</VirtualHost>
```

#### nginx
The specific configuration to add under your `server` section is:
```
location /_matrix/client/r0/user_directory {
    proxy_pass http://0.0.0.0:8090/_matrix/client/r0/user_directory;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $remote_addr;
}
```

Your `server` section should now look like this:
```
server {
    listen 443 ssl;
    server_name example.org;
    
    ...
    
    location /_matrix/client/r0/user_directory {
        proxy_pass http://localhost:8090/_matrix/client/r0/user_directory;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
    }
    
    location /_matrix/identity {
        proxy_pass http://localhost:8090/_matrix/identity;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
    }
    
    location /_matrix {
        proxy_pass http://localhost:8008/_matrix;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
    }
}
```

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

### Backends
#### LDAP
To ensure Directory feature works, here's how the LDAP configuration should look like:
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
Only include the `attribute` sub-sections if you would like to set another value. Else, it is best not to include them
to inherit the default values.

If you would like to include an attribute which is not a display name or a 3PID, you can use the
`directory.attribute.other` to list any extra attributes you want included in searches. If you do not want to include
any extra attribute, that configuration section can be skipped. 

#### SQL
If you plan to integrate directory search directly with synapse, use the `synapseSql` provider, based on the following
config:
```
synapseSql:
  enabled: true
  type: <database ID>
  connection: '<connection info>'
```
`type` and `connection`, including any other configuration item, follow the same values as the regular [SQL backend](../backends/sql.md).

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

#### Wordpress
See the [dedicated document](../backends/wordpress.md)

## Next steps
### Homeserver results
You can configure if the Homeserver should be queried at all when doing a directory search.  
To disable Homeserver results, set the following in mxisd config file:
```
directory.exclude.homeserever: true
```
