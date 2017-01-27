![Travis-CI build status](https://travis-ci.org/kamax-io/mxisd.svg?branch=master)

# Introduction
mxisd is an implementation of the Matrix Identity Server which aims to provide an alternative
to [sydent](https://github.com/matrix-org/sydent) and an external validation implementation of the
[Identity Service API](http://matrix.org/docs/spec/identity_service/unstable.html).

mxisd is currently in read-only mode with the following lookup strategy:
- LDAP backend: lookup the Matrix ID from an configurable attribute.
- Root Matrix Identity servers: If no hit in LDAP, proxy the request to the root servers.

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
- Edit an entity in your LDAP database and set the configure attribute with a Matrix ID (@johndoe:example.org)

## Run
Start the server in foreground with configuration location info `./build/libs/mxisd --spring.config.location=../../`

You should see a public key with `curl http://localhost:8090/_matrix/identity/api/v1/pubkey/ed25519%3A0`

You should see some JSON data with `curl http://localhost:8090/_matrix/identity/api/v1/lookup?medium=email&address=johndoe@example.org`

If you plan on testing the integration with a homeserver, you will need to run an HTTPS reverse proxy in front of it
as the homeserver implementation seems to require a HTTPS connection to an ID server.

# Install
1. Create a dedicated user: `useradd -r mxisd`
- Create config directory: `mkdir /etc/mxis`
- Change user ownership of `/etc/mxis` to dedicated user: `chown mxisd /etc/mxis`
- Copy `./build/libs/mxisd` to `/usr/bin/mxisd`: `sudo cp ./build/libs/mxisd /usr/bin/mxisd`
- Copy (or create a new) `./application.yaml` to `/etc/mxis/mxisd.yaml`
- Configure `/etc/mxis/mxisd.yaml` with production value - key.path being the most important - `/etc/mxis/signing.key` is recommended
- Copy `main/systemd/mxisd.service` to `/etc/systemd/system/` and edit as needed
- Enable service: `systemctl enable mxisd`
- Start service: `systemctl start mxisd`

# TODO
- Deb package
- Auto-discovery of matrix ids based on server name and username-like attribute
