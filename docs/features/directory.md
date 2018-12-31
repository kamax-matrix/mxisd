# User Directory
- [Description](#description)
- [Overview](#overview)
- [Requirements](#requirements)
- [Configuration](#configuration)
  - [Reverse Proxy](#reverse-proxy)
    - [Apache2](#apache2)
    - [nginx](#nginx)
  - [DNS Overwrite](#dns-overwrite)
- [Next steps](#next-steps)

## Description
This feature allows you to search for existing and/or potential users that are already present in your Identity backend
or that already share a room with you on the Homeserver.

Without any integration, synapse:
- Only search within the users **already** known to you or in public rooms
- Only search on the Display Name and the Matrix ID

By enabling this feature, you can by default:
- Search on Matrix ID, Display name and 3PIDs (Email, phone numbers) of any users already in your configured backend
- Search for users which you are not in contact with yet. Super useful for corporations who want to give Matrix access
internally, so users can just find themselves **prior** to having any common room(s)
- Add extra attributes of your backend to extend the search
- Include your homeserver search results to those found by mxisd

By integrating mxisd, you get the default behaviour and a bunch of extras, ensuring your users will always find each other.

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
2. Identity stores are queried for any match on the search value sent by the client.
3. The Homeserver, from which the request was intercepted, is queried using the same request as the client.
   Its address is resolved using the DNS Overwrite feature to reach its internal address on a non-encrypted port.
4. Results from Identity stores and the Homeserver are merged together and sent back to the client, believing it was the HS
which directly answered the request.

## Requirements
- Reverse proxy setup, which you should already have in place if you use mxisd
- At least one compatible [Identity store](../stores/README.md) enabled
  
## Configuration
### Reverse Proxy
#### Apache2
The specific configuration to put under the relevant `VirtualHost`:
```apache
ProxyPass /_matrix/client/r0/user_directory/ http://0.0.0.0:8090/_matrix/client/r0/user_directory/
```
`ProxyPreserveHost` or equivalent must be enabled to detect to which Homeserver mxisd should talk to when building
results.

Your `VirtualHost` should now look like this:
```apache
<VirtualHost *:443>
    ServerName example.org
    
    ...
    
    ProxyPreserveHost on
    ProxyPass /_matrix/client/r0/user_directory/ http://localhost:8090/_matrix/client/r0/user_directory/
    ProxyPass /_matrix/identity http://localhost:8090/_matrix/identity
    ProxyPass /_matrix http://localhost:8008/_matrix
</VirtualHost>
```

#### nginx
The specific configuration to add under your `server` section is:
```nginx
location /_matrix/client/r0/user_directory {
    proxy_pass http://0.0.0.0:8090/_matrix/client/r0/user_directory;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $remote_addr;
}
```

Your `server` section should now look like this:
```nginx
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
```yaml
dns:
  overwrite:
    homeserver:
      client:
        - name: 'example.org'
          value: 'http://localhost:8008'
```
- `name` must be the hostname of the URL that clients use when connecting to the Homeserver.
- `value` is the base internal URL of the Homeserver, without any `/_matrix/..` or trailing `/`.

## Next steps
### Homeserver results
You can configure if the Homeserver should be queried at all when doing a directory search.  
To disable Homeserver results, set the following in mxisd configuration file:
```yaml
directory:
  exclude:
    homeserver: true
```

### 3PID exclusion in search
You can configure if the 3PID should also be included when doing a directory search.
By default, a search is performed on the 3PIDs. If you would like to not include them:
```yaml
directory:
  exclude:
    threepid: true
```
