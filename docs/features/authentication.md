# Authentication
- [Description](#description)
- [Basic](#basic)
  - [Overview](#overview)
  - [synapse](#synapse)
  - [mxisd](#mxisd)
  - [Validate](#validate)
  - [Next steps](#next-steps)
    - [Profile auto-fil](#profile-auto-fill)
- [Advanced](#advanced)
  - [Overview](#overview-1)
  - [Requirements](#requirements)
  - [Configuration](#configuration)
    - [Reverse Proxy](#reverse-proxy)
      - [Apache2](#apache2)
    - [DNS Overwrite](#dns-overwrite)

## Description
Authentication is an enhanced feature of mxisd to ensure coherent and centralized identity management.  
It allows to use Identity stores configured in mxisd to authenticate users on your Homeserver.

Authentication is divided into two parts:
- [Basic](#basic): authenticate with a regular username.
- [Advanced](#advanced): same as basic with extra abilities like authenticate using a 3PID or do username rewrite.

## Basic
Authentication by username is possible by linking synapse and mxisd together using a specific module for synapse, also
known as password provider.

### Overview
An overview of the Basic Authentication process:
```
                                                                                    Identity stores
 Client                                                                             +------+
   |                                            +-------------------------+    +--> | LDAP |
   |   +---------------+  /_matrix/identity     | mxisd                   |    |    +------+
   +-> | Reverse proxy | >------------------+   |                         |    |
       +--|------------+                    |   |                         |    |    +--------+
          |                                 +-----> Check ID stores     >------+--> | SQL DB |
     Login request                          |   |                         |    |    +--------+
          |                                 |   |     |                   |    |
          |   +--------------------------+  |   +-----|-------------------+    +-->  ...
          +-> | Homeserver               |  |         |
              |                          |  |         |
              | - Validate credentials >----+         |
              |   Using REST auth module |            |
              |                          |            |
              | - Auto-provision <-------------------<+
              |   user profiles          |    If valid credentials and supported by Identity store(s)
              +--------------------------+
```
Performed on [synapse with REST auth module](https://github.com/kamax-io/matrix-synapse-rest-auth/blob/master/README.md)

### Synapse
- Install the [password provider](https://github.com/kamax-io/matrix-synapse-rest-auth)
- Edit your **synapse** configuration:
  - As described by the auth module documentation
  - Set `endpoint` to `http://mxisdAddress:8090` - Replace `mxisdAddress` by an IP/host name that provides a direct
  connection to mxisd.  
  This **MUST NOT** be a public address, and SHOULD NOT go through a reverse proxy.
- Restart synapse

### mxisd
- Configure and enable at least one [Identity store](../stores/README.md)
- Restart mxisd

### Validate
Login on the Homeserver using credentials present in one of your Identity stores.

## Next steps
### Profile auto-fill
Auto-filling user profile depends on its support by your configured Identity stores.  
See your Identity store [documentation](../stores/README.md) on how to enable the feature.


## Advanced
The Authentication feature allows users to:
- Rewrite usernames matching a pattern to be mapped to another username via a 3PID.
- login to their Homeserver by using their 3PIDs in a configured Identity store.

This feature also allows to work around the following issues:
- Lowercase all usernames for synapse, allowing case-insensitive login
- Unable to login on synapse if username is numerical
- Any generic transformation of username prior to sending to synapse, bypassing the restriction that password providers
cannot change the localpart being authenticated.

### Overview
This is performed by intercepting the Homeserver endpoint `/_matrix/client/r0/login` as depicted below:
```
            +----------------------------+
            |  Reverse Proxy             |
            |                            |
            |                            |     Step 1    +---------------------------+     Step 2
            |                            |               |                           |
Client+---->| /_matrix/client/r0/login +---------------->|                           | Look up address  +---------+
            |                      ^     |               |  mxisd - Identity server  +----------------->| Backend |
            |                      |     |               |                           |                  +---------+
            | /_matrix/* +--+      +---------------------+                           |
            |               |            |               +---------------+-----------+
            |               |            |     Step 4                    |
            |               |            |                               | Step 3
            +---------------|------------+                               |
                            |                                            | /_matrix/client/r0/login
                            |                       +--------------+     |
                            |                       |              |     |
                            +---------------------->|  Homeserver  |<----+
                                                    |              |
                                                    +--------------+

```

Steps of user authentication using a 3PID:
1. The intercepted login request is directly sent to mxisd instead of the Homeserver.
2. Identity stores are queried for a matching user identity in order to modify the request to use the user name.
3. The Homeserver, from which the request was intercepted, is queried using the request at previous step.
   Its address is resolved using the DNS Overwrite feature to reach its internal address on a non-encrypted port.
4. The response from the Homeserver is sent back to the client, believing it was the HS which directly answered.

### Requirements
- Compatible [Identity store](../stores/README.md)
- [Basic Authentication configured and working](#basic)
- Client and Homeserver using the [C2S API r0.4.x](https://matrix.org/docs/spec/client_server/r0.4.0.html) or later
- Reverse proxy setup

### Configuration
#### Reverse Proxy
##### Apache2
The specific configuration to put under the relevant `VirtualHost`:
```apache
ProxyPass /_matrix/client/r0/login http://localhost:8090/_matrix/client/r0/login
```
`ProxyPreserveHost` or equivalent **must** be enabled to detect to which Homeserver mxisd should talk to when building results.

Your VirtualHost should now look similar to:
```apache
<VirtualHost *:443>
    ServerName example.org
    
    ...
    
    ProxyPreserveHost on
    ProxyPass /_matrix/client/r0/login http://localhost:8090/_matrix/client/r0/login
    ProxyPass /_matrix/identity http://localhost:8090/_matrix/identity
    ProxyPass /_matrix http://localhost:8008/_matrix
</VirtualHost>
```

##### nginx

The specific configuration to add under the relevant `server`:

```nginx
location /_matrix/client/r0/login {
    proxy_pass http://localhost:8090;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $remote_addr;
}
```

Your `server` section should now look similar to:

```nginx
server {
    listen 443 ssl;
    server_name matrix.example.org;
    
    # ...
    
    location /_matrix/client/r0/login {
        proxy_pass http://localhost:8090;
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

#### DNS Overwrite

Just like you need to configure a reverse proxy to send client requests to mxisd, you also need to configure mxisd with
the internal IP of the Homeserver so it can talk to it directly to integrate its directory search.

To do so, put the following configuration in your mxisd configuration:
```yaml
dns:
  overwrite:
    homeserver:
      client:
        - name: 'example.org'
          value: 'http://localhost:8008'
```
`name` must be the hostname of the URL that clients use when connecting to the Homeserver.
You can use `${server.name}` to auto-populate the `value` using the `server.name` configuration option and avoid duplicating it.
In case the hostname is the same as your Matrix domain and `server.name` is not explicitely set in the config, `server.name` will default to
`matrix.domain` and will still probably have the correct value.

`value` is the base internal URL of the Homeserver, without any `/_matrix/..` or trailing `/`.

### Optional features

The following features are available after you have a working Advanced setup:

- Username rewrite: Allows you to rewrite the username of a regular login/pass authentication to a 3PID, that then gets resolved using the regular lookup process. Most common use case is to allow login with numerical usernames on synapse, which is not possible out of the box.

#### Username rewrite
In mxisd config:
```yaml
auth:
  rewrite:
    user:
      rules:
        - regex: <your regexp>
          medium: 'your.custom.medium.type'
```
`rules` takes a list of rules. Rules have two properties:
- `regexp`: The regex pattern to match. This **MUST** match the full string. See [Java regex](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) for syntax.
- `medium`: Custom 3PID type that will be used in the 3PID lookup. This can be anything you want and needs to be supported
by your Identity store config and/or code.

Rules are matched in listed order.

Common regexp patterns:
- Numerical usernames: `[0-9]+`

##### LDAP Example
If your users use their numerical employee IDs, which cannot be used with synapse, you can make it work with (relevant config only):
```yaml
auth:
  rewrite:
    user:
      rules:
        - regex: '[0-9]+'
          medium: 'kmx.employee.id'
          
ldap:
  attribute:
    threepid:
      kmx.employee.id:
        - 'ldapAttributeForEmployeeId'
```
