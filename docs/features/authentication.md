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
### Synapse
You will need:
- Configure and enable at least one [Identity store](../backends/)
- Install the [REST auth module](https://github.com/kamax-io/matrix-synapse-rest-auth)

Once installed, edit your synapse configuration as described for the auth module:
- Set `endpoint` to `http://mxisdAddress:8090` - Replace `mxisdAddress` to an internal IP/Hostname.
- If you want to avoid [known issues](https://github.com/matrix-org/matrix-doc/issues/586) with lower/upper case
usernames, set `enforceLowercase` in the REST config to `true`.

**IMPORTANT**: if this is a new installation, it is highly recommended to enforce lowercase, as it is not possible to
workaround the bug at a later date and will cause issues with invites, searches, authentication.

Restart synapse and login on the Homeserver using credentials present in your backend.  

## Profile auto-fill
Auto-filling user profile depends on two conditions:
- The REST auth module is configured for it, which is the case by default
- Your Identity store is configured to provide profile data. See your Identity store [documentation](../backends/) on
how to enable the feature.
