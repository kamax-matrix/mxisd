# Identity service Federation
## Overview
```
              +-------------------+   +-------------> +----------+
              | mxisd             |   |               | Backends |
              |                   |   |      +------> +----------+
              |                   |   |      |
              | Invites / Lookups |   |      |
 Federated    | +--------+        |   |      |        +-------------------+
 Identity  ---->| Remote |>-----------+      +------> | Remote Federated  |
 Server       | +--------+        |          |        | mxisd servers     |
              |                   |          |        +-------------------+
              | +--------+        |          |
 Homeserver --->| Local  |>------------------+
 and clients  | +--------+        |          |        +--------------------------+ 
              +-------------------+          +------> | Central Identity service |
                                                      | Matrix.org / Vector.im   |
                                                      +--------------------------+
```
To allow other federated Identity Server to reach yours, the same algorithm used for Homeservers takes place:
1. Check for the appropriate DNS SRV record
2. If not found, use the base domain

## Configuration
If your Identity Server public hostname does not match your Matrix domain, configure the following DNS SRV entry 
and replace `matrix.example.com` by your Identity server public hostname - **Make sure to end with a final dot!**
```
_matrix-identity._tcp.example.com. 3600 IN SRV 10 0 443 matrix.example.com.
``` 
This would only apply for 3PID that are DNS-based, like e-mails. For anything else, like phone numbers, no federation 
is currently possible.  

The port must be HTTPS capable which is what you get in a regular setup with a reverse proxy from 443 to TCP 8090 of mxisd.
