# Authentication
Authentication is an enchanced Identity feature of mxisd to ensure coherent and centralized identity management.

It allows to use Identity stores configured in mxisd to authenticate users on your Homeserver.

## Overview
```
                                                                                    Backends
 Client                                                                             +------+
   |                                            +-------------------------+    +--> | LDAP |
   |   +---------------+  /_matrix/identity     | mxisd                   |    |    +------+
   +-> | Reverse proxy | >------------------+   |                         |    |
       +--|------------+                    |   |                         |    |    +--------+
          |                                 +-----> Check wiht backends >------+--> | SQL DB |
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
