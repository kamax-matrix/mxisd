# Authentication
Performed via [synapse with REST auth module](https://github.com/kamax-io/matrix-synapse-rest-auth/blob/master/README.md)  
Point the `endpoint` to mxisd internal IP on port 8090

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

## Profile auto-fill
To be documented