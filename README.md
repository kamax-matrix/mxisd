mxisd
-----
![Travis-CI build status](https://travis-ci.org/kamax-io/mxisd.svg?branch=master)  

[Overview](#overview) | [Lookup process](#lookup-process) | [Packages](#packages) | [From source](#from-source) | [Network Discovery](#network-discovery) | [Integration](#integration) | [Support](#support)

# Overview
mxisd is an implementation of the [Matrix Identity Service specification](https://matrix.org/docs/spec/identity_service/unstable.html) 
which provides an alternative to the vector.im and matrix.org Identity servers.

mxisd is federated and uses a cascading lookup model which performs lookup from a more authoritative to a less authoritative source, usually doing:
- Local identity stores: LDAP, etc.
- Federated identity stores: another Identity Server in charge of a specific domain, if applicable
- Configured identity stores: another Identiy Server specifically configured, if part of some sort of group trust
- Root identity store: vector.im/matrix.org central Identity Server

mxisd only aims to support workflow that does NOT break federation or basic lookup process of the Matrix ecosystem.

**NOTE:** mxisd is **NOT** an authenticator module for homeservers. Identity servers are opaque public directories for those who publish their data on it.  
For more details, please read the [Identity Service specification](https://matrix.org/docs/spec/identity_service/unstable.html).

# Lookup Process
Default Lookup strategy will use a priority order and a configurable recursive/local type of request.

## E-mail
Given the 3PID `john.doe@example.org`, the following will be performed until a mapping is found:
- LDAP: lookup the Matrix ID (partial or complete) from a configurable attribute using a dedicated query.
- DNS: lookup another Identity Server using the domain part of an e-mail and:
  - Look for a SRV record under `_matrix-identity._tcp.example.org`
  - Lookup using the base domain name `example.org`
- Forwarder: Proxy the request to other configurable identity servers.

## Phone number
Given the phone number `+123456789`, the following lookup logic will be performed:
- LDAP: lookup the Matrix ID (partial or complete) from a configurable attribute using a dedicated query.
- Forwarder: Proxy the request to other configurable identity servers.

# Packages
## Native installer
See releases for native installers of supported systems.  
If none is available, please use other packages or build from source.

## Docker
### From source
```
docker build -t your-org/mxisd:$(git describe --tags --always --dirty) .
docker run -v /data/mxisd/etc:/etc/mxisd -v /data/mxisd/var:/var/mxisd -t your-org/mxisd:$(git describe --tags --always --dirty)
```

## Debian
TODO

# From Source
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

## Test build and configuration
Start the server in foreground to validate the build:
```
java -jar build/libs/mxisd.jar
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

## Install
After [building](#build) the software, run all the following commands as `root` or using `sudo`
1. Prepare files and directories:
```
# Create a dedicated user
useradd -r mxisd

# Create bin directory
mkdir /opt/mxisd

# Create config directory and set ownership
mkdir /etc/mxisd
chown mxisd /etc/mxisd

# Create data directory and set ownership
mkdir /var/opt/mxisd
chown mxisd /var/opt/mxisd

# Copy <repo root>/build/libs/mxisd.jar to bin directory
cp ./build/libs/mxisd.jar /opt/mxisd/
chown mxisd /opt/mxisd/mxisd.jar
chmod a+x /opt/mxisd/mxisd.jar

# Create symlink for easy exec
ln -s /opt/mxisd/mxisd.jar /usr/bin/mxisd
```

2. Copy the config file created earlier `./application.yaml` to `/etc/mxisd/mxisd.yaml`
3. Configure `/etc/mxisd/mxisd.yaml` with production value, `key.path` being the most important - `/var/opt/mxisd/signing.key` is recommended
4. Copy `<repo root>/src/main/systemd/mxisd.service` to `/etc/systemd/system/` and edit if needed
5. Manage service for auto-startup
```
# Enable service
systemctl enable mxisd

# Start service
systemctl start mxisd
```

# Network Discovery
To allow other federated Identity Server to reach yours, configure the following DNS SRV entry (adapt to your values):
```
_matrix-identity._tcp.example.com. 3600 IN SRV 10 0 443 matrix.example.com.
```
This would only apply for 3PID that are DNS-based, like e-mails. For anything else, like phone numbers, no federation is currently possible.  

The port must be HTTPS capable. Typically, the port `8090` of mxisd should be behind a reverse proxy which does HTTPS.
See the [integration section](#integration) for more details.

# Integration
- [HTTPS and Reverse proxy](https://github.com/kamax-io/mxisd/wiki/HTTPS)
- [synapse](https://github.com/kamax-io/mxisd/wiki/Homeserver-Integration)

# Support
## Community
If you need help, want to report a bug or just say hi, you can reach us on Matrix at [#mxisd:kamax.io](https://matrix.to/#/#mxisd:kamax.io)  
For more high-level discussion about the Identity Server architecture/API, go to [#matrix-identity:matrix.org](https://matrix.to/#/#matrix-identity:matrix.org)

## Professional
If you would prefer professional support/custom development for mxisd and/or for Matrix in general, including other open source technologies/products, 
please visit [our website](https://www.kamax.io/) to get in touch with us and get a quote.
