# Authentication

- [Description](#description)
- [Overview](#overview)
- [Getting started](#getting-started)
  - [Synapse](#synapse)
  - [mxisd](#mxisd)
  - [Validate](#validate)
- [Next steps](#next-steps)
  - [Profile auto-fil](#profile-auto-fill)
- [Advanced Authentication](#advanced-authentication)
  - [Requirements](#requirements)
  - [Configuration](#configuration)
    - [Reverse Proxy](#reverse-proxy)
      - [Apache2](#apache2)
    - [DNS Overwrite](#dns-overwrite)
    - [Backends](#backends) 

## Description
Authentication is an enhanced Identity feature of mxisd to ensure coherent and centralized identity management.

It allows to use Identity stores configured in mxisd to authenticate users on your Homeserver.

This feature can also provide the ability to users to login on the Homeserver using their third party identities (3PIDs) provided by an Identity store.

## Overview
An overview of the Authentication process is depicted below: 

```
                                                                                    Backends
 Client                                                                             +------+
   |                                            +-------------------------+    +--> | LDAP |
   |   +---------------+  /_matrix/identity     | mxisd                   |    |    +------+
   +-> | Reverse proxy | >------------------+   |                         |    |
       +--|------------+                    |   |                         |    |    +--------+
          |                                 +-----> Check with backends >------+--> | SQL DB |
     Login request                          |   |                         |    |    +--------+
          |                                 |   |     |                   |    |
          |   +--------------------------+  |   +-----|-------------------+    +-->  Others
          +-> | Homeserver               |  |         |
              |                          |  |         |
              | - Validate credentials >----+         |
              |   Using REST auth module |            |
              |                          |            |
              | - Auto-provision <-------------------<+
              |   user profiles          |    If valid credentials and supported by backend
              +--------------------------+
```
Performed on [synapse with REST auth module](https://github.com/kamax-io/matrix-synapse-rest-auth/blob/master/README.md)

## Getting started
Authentication is possible by linking synapse and mxisd together using the REST auth module
(also known as password provider).

### Synapse
- Install the [REST auth module](https://github.com/kamax-io/matrix-synapse-rest-auth).
- Edit your synapse configuration:
  - As described by the auth module documentation
  - Set `endpoint` to `http://mxisdAddress:8090` - Replace `mxisdAddress` by an IP/host name that provides a direct
  connection to mxisd.  
  This **MUST NOT** be a public address, and SHOULD NOT go through a reverse proxy.
- Restart synapse

### mxisd
- Configure and enable at least one [Identity store](../backends/)
- Restart mxisd

### Validate
Login on the Homeserver using credentials present in your backend.

## Next steps
### Profile auto-fill
Auto-filling user profile depends on two conditions:
- The REST auth module is configured for it, which is the case by default
- Your Identity store is configured to provide profile data. See your Identity store [documentation](../backends/) on
how to enable the feature.


## Advanced Authentication
The Authentication feature allows users to login to their Homeserver by using their 3PIDs registered in an available Identity store.

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
2. Enabled backends are queried for a matching user identity in order to modify the request to use the user name.
3. The Homeserver, from which the request was intercepted, is queried using the request at previous step. Its address is resolved using the DNS Overwrite feature to reach its internal address on a non-encrypted port.
4. The response from the Homeserver is sent back to the client, believing it was the HS which directly answered.

### Requirements
- Reverse proxy setup
- Homeserver
- Compatible Identity backends:
	- LDAP
	- SQL
	- REST

### Configuration

#### Reverse Proxy

##### Apache2
The specific configuration to put under the relevant `VirtualHost`:
```
ProxyPass /_matrix/client/r0/login http://localhost:8090/_matrix/client/r0/login
```
`ProxyPreserveHost` or equivalent must be enabled to detect to which Homeserver mxisd should talk to when building results.

Your VirtualHost should now look like this:
```
<VirtualHost *:443>
    ServerName example.org
    
    ...
    
    ProxyPreserveHost on
    ProxyPass /_matrix/client/r0/login http://localhost:8090/_matrix/client/r0/login
    ProxyPass /_matrix/identity/ http://localhost:8090/_matrix/identity/
    ProxyPass /_matrix/ http://localhost:8008/_matrix/
</VirtualHost>
```

#### DNS Overwrite
Just like you need to configure a reverse proxy to send client requests to mxisd, you also need to configure mxisd with the internal IP of the Homeserver so it can talk to it directly to integrate its directory search.


To do so, put the following configuration in your `application.yaml`:
```
dns.overwrite.homeserver.client:
  - name: 'example.org'
    value: 'http://localhost:8008'
```
`name` must be the hostname of the URL that clients use when connecting to the Homeserver.
In case the hostname is the same as your Matrix domain, you can use `${matrix.domain}` to auto-populate the `value` using the `matrix.domain` configuration option and avoid duplicating it.

value is the base internal URL of the Homeserver, without any /_matrix/.. or trailing /.

#### Backends
The Backends should be configured as described in the documentation of the [Directory User](directory-users.md) feature. 









