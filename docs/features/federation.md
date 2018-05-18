# Federation
Federation is the process by which domain owners can make compatible 3PIDs mapping auto-discoverable by looking for another
Federated Identity server using the DNS domain part of the 3PID.

Emails are the best candidate for this kind of resolution which are DNS domain based already.  
On the other hand, Phone numbers cannot be resolved this way.

For 3PIDs which are not compatible with the DNS system, mxisd can be configured to talk to fallback Identity servers like
the central matrix.org one. See the [Identity feature](identity.md#lookups) for instructions on how to enable it.

Outbound federation is enabled by default while inbound federation is opt-in and require a specific DNS record.

## Overview
```
              +-------------------+   +-------------> +----------+
              | mxisd             |   |               | Backends |
              |                   |   |      +------> +----------+
              |                   |   |      |
              | Invites / Lookups |   |      |
 Federated    | +--------+        |   |      |
 Identity  ---->| Remote |>-----------+      |
 Server       | +--------+        |          |
              |                   |          |
              | +--------+        |          |        +-------------------+
 Homeserver --->| Local  |>------------------+------> | Remote Federated  |
 and clients  | +--------+        |                   | mxisd servers     |
              +-------------------+                   +-------------------+
```

## Inbound
If you would like to be reachable for lookups over federation, create the following DNS SRV entry and replace
`matrix.example.com` by your Identity server public hostname:
```
_matrix-identity._tcp.example.com. 3600 IN SRV 10 0 443 matrix.example.com.
``` 

The port must be HTTPS capable which is what you get in a regular setup with a reverse proxy from 443 to TCP 8090 of mxisd.

## Outbound
If you would like to disable outbound federation and isolate your identity server from the rest of the Matrix network,
use the following mxisd configuration options:
```yaml
lookup.recursive.enabled: false
invite.resolution.recursive: false
session.policy.validation.forLocal.toRemote.enabled: false
session.policy.validation.forRemote.toRemote.enabled: false
``` 

There is currently no way to selectively disable federation towards specific servers, but this feature is planned.
