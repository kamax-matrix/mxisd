# Architecture
## Overview
### Basic setup without integration or incoming federation
```
 Client
   |
TCP 443
   |   +---------------------+            +---------------------------+
   +-> | Reverse proxy       |            | Homeserver                |
       |                     | TCP 8008   |                           |
       |  /_matrix/* -------------------> | - 3PID invite from client |
       |                     |            |   |                       |
       |  /_matrix/identity/ |            |   |                       |
       +--|------------------+            +---|-----------------------+
          |                                   |
          +<---------------------------------<+
          |                                          Backends
          |   +-------------------+                  +------+    +--------+
 TCP 8090 +-> | mxisd             |          +-----> | LDAP | -> | SQL DB |
              |                   |          |       +------+    +--------+ ....
              | - Profile's 3PIDs >----+     |
              | - 3PID Invites    |    |     |
              +-|-----------------+    +>----+
                |                      |     |       +--------------------------+
                |                      |     |       | Central Identity service |
                +>-------------------->+     +-----> | Matrix.org / Vector.im   |
                |                            TCP 443 +--------------------------+
             TCP 443
                |  +------------------------+
                |  | Remote Federated       |
                |  | mxisd servers          |
                |  |                        |
                +--> - 3PID Invites         |
                   +------------------------+
```
### With Authentication
See the [dedicated document](features/authentication.md).

### With Directory
See the [dedicated document](features/directory-users.md).

### With Federation
See the [dedicated document](features/federation.md).
