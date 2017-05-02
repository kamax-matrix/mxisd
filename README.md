![Travis-CI build status](https://travis-ci.org/kamax-io/mxisd.svg?branch=master)

# Introduction
mxisd is an implementation of the Matrix Identity Server which aims to provide an alternative
to [sydent](https://github.com/matrix-org/sydent) and an external validation implementation of the
[Identity Service API](http://matrix.org/docs/spec/identity_service/unstable.html).

# Scope
mxisd is a federated Matrix Identity Server following a cascading lookup model, using LDAP then other identity servers, including the central Matrix servers.

mxisd is currently read-only, implementation to bind 3PID will follow shortly.

## Contact
If you need help, want to report a bug or just say hi, you can reach us at [#mxisd:kamax.io](https://matrix.to/#/#mxisd:kamax.io)

For more high-level discussion about the Identity Server architecture/API, go to [#matrix-identity:matrix.org](https://matrix.to/#/#matrix-identity:matrix.org)

## How does it work
Default Lookup strategy will use a priority order and a configurable recursive/local type of request.

### E-mail
Given the 3PID `john.doe@example.org`, the following will be performed until a mapping is found:
- LDAP: lookup the Matrix ID (partial or complete) from a configurable attribute using a dedicated query.
- DNS: lookup another Identity Server using the domain part of an e-mail and:
  - Look for a SRV record under `_matrix-identity._tcp.example.org`
  - Lookup using the base domain name `example.org`
- Forwarder: Proxy the request to other configurable identity servers.

### Phone number
Given the phone number `+123456789`, the following lookup logic will be performed:
- LDAP: lookup the Matrix ID (partial or complete) from a configurable attribute using a dedicated query.
- Forwarder: Proxy the request to other configurable identity servers.

# Quick start
## Requirements
- JDK 1.8

## Build
```
git clone https://github.com/kamax-io/mxisd.git
cd mxisd
./gradlew build
```

## Configure
1. Create a new local config: `cp application.example.yaml application.yaml`
2. Set the `server.name` value to the domain value used in your Home Server configuration
3. Provide the LDAP attributes you want to use for lookup
4. Edit an entity in your LDAP database and set the configure attribute with a Matrix ID (e.g. `@john.doe:example.org`)

## Run
Start the server in foreground:
```
./gradlew bootRun
```

Ensure the signing key is available:
```
curl http://localhost:8090/_matrix/identity/api/v1/pubkey/ed25519:0
```

Validate your LDAP config and binding info (replace the e-mail):
```
curl "http://localhost:8090/_matrix/identity/api/v1/lookup?medium=email&address=john.doe@example.org"
```

If you plan on testing the integration with a homeserver, you will need to run an HTTPS reverse proxy in front of it
as the reference Home Server implementation [synapse](https://github.com/matrix-org/synapse) requires a HTTPS connection
to an ID server.  
See the [Integration section](https://github.com/kamax-io/mxisd#integration) for more details.

# Install
After [building](#build) the software, run all the following commands as `root` or using `sudo`

1. Create a dedicated user: `useradd -r mxisd`
2. Create config directory: `mkdir /etc/mxis`
3. Change user ownership of `/etc/mxis` to dedicated user: `chown mxisd /etc/mxis`
4. Copy `<repo root>/build/libs/mxisd.jar` to `/usr/bin/mxisd`: `cp ./build/libs/mxisd.jar /usr/bin/mxisd`
5. Make it executable: `chmod a+x /usr/bin/mxisd`
6. Copy (or create a new) `./application.yaml` to `/etc/mxis/mxisd.yaml`
7. Configure `/etc/mxis/mxisd.yaml` with production value, `key.path` being the most important - `/etc/mxis/mxisd-signing.key` is recommended
8. Copy `<repo root>/main/systemd/mxisd.service` to `/etc/systemd/system/` and edit if needed
9. Enable service: `systemctl enable mxisd`
10. Start service: `systemctl start mxisd`

# Integration
- [synapse](https://github.com/kamax-io/mxisd/wiki/Synapse-Integration)

# Docker
- https://github.com/doofy/mxisd-docker

# TODO
- Deb package
