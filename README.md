![Travis-CI build status](https://travis-ci.org/kamax-io/mxisd.svg?branch=master)

# Introduction
mxisd is an implementation of the Matrix Identity Server which aims to provide an alternative
to [sydent](https://github.com/matrix-org/sydent) and an external validation implementation of the
[Identity Service API](http://matrix.org/docs/spec/identity_service/unstable.html).

mxisd is currently in read-only mode and use a priority lookup strategy with several providers.

Given the 3PID `john.doe@example.org`, the following would be performed in priority order until a mapping is found:
- LDAP: lookup the Matrix ID from a configurable attribute.
- DNS: lookup another Identity Server using the domain part of an e-mail and:
  - Look for a SRV record under `_matrix-identity._tcp.example.org`
  - Lookup using the base domain name `example.org`
- Forwarder: Proxy the request to other identity servers (`matrix.org` and `vector.im` currently hardcoded).

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
- Edit `application.yaml` to your needs - at least provide the LDAP attributes
- Edit an entity in your LDAP database and set the configure attribute with a Matrix ID (e.g. `@john.doe:example.org`)

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
as the homeserver implementation seems to require a HTTPS connection to an ID server.

# Install
Run all the following commands as `root` or via `sudo`:
1. Create a dedicated user: `useradd -r mxisd`
- Create config directory: `mkdir /etc/mxis`
- Change user ownership of `/etc/mxis` to dedicated user: `chown mxisd /etc/mxis`
- Copy `<repo root>/build/libs/mxisd` to `/usr/bin/mxisd`: `cp ./build/libs/mxisd /usr/bin/mxisd`
- Make it executable: `chmod a+x /usr/bin/mxisd`
- Copy (or create a new) `./application.yaml` to `/etc/mxis/mxisd.yaml`
- Configure `/etc/mxis/mxisd.yaml` with production value - key.path being the most important - `/etc/mxis/signing.key` is recommended
- Copy `<repo root>/main/systemd/mxisd.service` to `/etc/systemd/system/` and edit as needed
- Enable service: `systemctl enable mxisd`
- Start service: `systemctl start mxisd`

# TODO
- Deb package
- Docker container
