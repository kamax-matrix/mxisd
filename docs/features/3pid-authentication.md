# 3pid-authentication

## Description
This features allows you to login to Homeserver by using a third party identifier (3PID) that is registered on your Identity backend.

## Overview
This is performed by intercepting the Homeserver endpoint `/_matrix/client/r0/login` as depicted below:

```
             +----------------+
             | Reverse Proxy
             |
             |                           Step 1                                    Step 2
             |                                   +---------------------------+
Client+--->  |/_matrix/client/r0/login +-------> |                           | Look up address  +---------+
             |                                   |  mxisd - Identity server  | +--------------> | Backend |
             |                         <-------+ |                           |                  +---------+
             |/_matrix/*                 Step 4  +---------------------------+
             |    +
             |    |                                            +    Step 3
             +    |                                            |
                  |                                            |    /_matrix/client/r0/login
                  |                                            |
                  |                                            |
                  |                                            v
                  |
                  |                              +---------------------------+
                  |                              |                           |
                  +----------------------------> |        Homeserver         |
                                                 |                           |
                                                 +---------------------------+
```

Steps:
1. The intercepted login request is directly sent to mxisd instead of the Homeserver.
2. If the request uses a third party identifier, enabled backends are queried for a matching user identity, and then the request is rewritten to use the user name from the result.
3. The Homeserver, from which the request was intercepted, is queried using the request from step 2. Its address is resolved using the DNS Overwrite feature to reach its internal addresss on a non-encrypted port.
4. The response from the Homeserver is sent back to the client, believing it was the HS which directly answered.

## Requirements
- Reverse proxy setup
- Homeserver
- Compatible Identity backends:
	- LDAP
	- SQL
	- REST

## Configuration

### Reverse Proxy

#### Apache2
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

### DNS Overwrite
Just like you need to configure a reverse proxy to send client requests to mxisd, you also need to configure mxisd with the internal IP of the Homeserver so it can talk to it directly to integrate its directory search.


To do so, put the following configuration in your `application.yaml`:
```
dns.overwrite.homeserver.client:
  - name: 'example.org'
    value: 'http://localhost:8008'
```
`name` must be the hostname of the URL that clients use when connecting to the Homeserver.
In case the hostname is the same as your Matrix domain, you can use `${matrix.domain}` to auto-populate the value using the `matrix.domain` configuration option and avoid duplicating it.

value is the base internal URL of the Homeserver, without any /_matrix/.. or trailing /.

### Backends
The Backends should be configured as described in the documentation of the [Directory User](directory-users.md) feature. 









