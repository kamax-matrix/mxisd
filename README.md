mxisd - Federated Matrix Identity Server Daemon
-----
![Travis-CI build status](https://travis-ci.org/kamax-io/mxisd.svg?branch=master)  

[Overview](#overview) | [Features](#features) | [Lookup process](#lookup-process) | [Packages](#packages) | 
[From source](#from-source) | [Configuration](#configuration) | [Network Discovery](#network-discovery) | 
[Integration](#integration) | [Support](#support)

# Overview
mxisd is a Federated Matrix Identity server aimed to self-hosted Matrix infrastructures.

mxisd uses a cascading lookup model which performs lookup from a more authoritative to a less authoritative source, usually doing:
- Local identity stores: LDAP, etc.
- Federated identity stores: another Identity Server in charge of a specific domain, if applicable
- Configured identity stores: another Identity Server specifically configured, if part of some sort of group trust
- Root identity store: vector.im/matrix.org central Identity Servers

mxisd provides an alternative to [sydent](https://github.com/matrix-org/sydent), while still connecting to the vector.im and matrix.org Identity servers, 
by implementing the [Matrix Identity Service specification](https://matrix.org/docs/spec/identity_service/unstable.html).

mxisd only aims to support workflows that do NOT break federation or basic lookup processes of the Matrix ecosystem.

# Features
- Single lookup of 3PID (E-mail, phone number, etc.) by the Matrix Client or Homeserver.
- Bulk lookups when trying to find possible matches within contacts in Android and iOS clients.
- Bind of 3PID by a Matrix user within a Matrix client.
- Support of invitation to rooms by e-mail with e-mail notification to invitee.
- Authentication support in [synapse](https://github.com/matrix-org/synapse) via the [REST auth module](https://github.com/kamax-io/matrix-synapse-rest-auth).

In the pipe:
- Support to proxy 3PID bindings in user profile to the central Matrix.org servers

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
See [releases]((https://github.com/kamax-io/mxisd/releases)) for native installers of supported systems.  
If none is available, please use other packages or build from source.

## Debian
### Download
See the [releases section](https://github.com/kamax-io/mxisd/releases).

### Configure and run
After installation:
1. Copy the sample config file `/etc/mxisd/mxisd-sample.yaml` to `/etc/mxisd/mxisd.yaml`
2. [Configure](#configuration)
3. Start the service: `sudo systemctl start mxisd`

### From source
Requirements:
- fakeroot
- dpkg-deb

Run:
```
./gradlew buildDeb 
```
You will find the debian package in `build/dist`

## Docker
```
docker pull kamax/mxisd
```

For more info, see [the public repository](https://hub.docker.com/r/kamax/mxisd/)
### From source
[Build mxisd](#build) then build the docker image:
```
docker build -t your-org/mxisd:$(git describe --tags --always --dirty) .
```
You can run a container of the given image and test it with the following command (adapt volumes host paths):
```
docker run -v /data/mxisd/etc:/etc/mxisd -v /data/mxisd/var:/var/mxisd -p 8090:8090 -t your-org/mxisd:$(git describe --tags --always --dirty)
```

# From Source
## Requirements
- JDK 1.8

## Build
```
git clone https://github.com/kamax-io/mxisd.git
cd mxisd
./gradlew build
```
then see the [Configuration](#configuration) section.

## Test build
Start the server in foreground to validate the build:
```
java -jar build/libs/mxisd.jar
```

Ensure the signing key is available:
```
$ curl http://localhost:8090/_matrix/identity/api/v1/pubkey/ed25519:0
{"public_key":"..."}
```

Test basic recursive lookup (requires Internet connection with access to TCP 443):
```
$ curl 'http://localhost:8090/_matrix/identity/api/v1/lookup?medium=email&address=mxisd-lookup-test@kamax.io'
{"address":"mxisd-lookup-test@kamax.io","medium":"email","mxid":"@mxisd-lookup-test:kamax.io",...}
```


If you enabled LDAP, you can also validate your config with a similar request after replacing the `address` value with something present within your LDAP
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

2. Copy the config file created earlier `./application.example.yaml` to `/etc/mxisd/mxisd.yaml`
3. [Configure](#configuration)
4. Copy `<repo root>/src/systemd/mxisd.service` to `/etc/systemd/system/` and edit if needed
5. Enable service for auto-startup
```
systemctl enable mxisd
```
6. Start mxisd
```
systemctl start mxisd
```

# Configuration
After following the specific instructions to create a config file from the sample:
1. Set the `matrix.domain` value to the domain value used in your Home Server configuration
2. Set an absolute location for the signing keys using `key.path`
3. Set a location for the default SQLite persistence using `storage.provider.sqlite.database`
4. Configure the E-mail invite sender with items starting in `invite.sender.email`

In case your IS public domain does not match your Matrix domain, see `server.name` and `server.publicUrl` 
config items.

If you want to use the LDAP backend:
1. Enable it with `ldap.enabled`
2. Configure connection options using items starting in `ldap.connection`
3. You may want to valid default values for `ldap.attribute` items


# Network Discovery
To allow other federated Identity Server to reach yours, configure the following DNS SRV entry (adapt to your values):
```
_matrix-identity._tcp.example.com. 3600 IN SRV 10 0 443 matrix.example.com.
```
This would only apply for 3PID that are DNS-based, like e-mails. For anything else, like phone numbers, no federation 
is currently possible.  

The port must be HTTPS capable. Typically, the port `8090` of mxisd should be behind a reverse proxy which does HTTPS.
See the [integration section](#integration) for more details.

# Integration
- [HTTPS and Reverse proxy](https://github.com/kamax-io/mxisd/wiki/HTTPS)
- [synapse](https://github.com/kamax-io/mxisd/wiki/Homeserver-Integration) as Identity server
- [synapse with REST auth module](https://github.com/kamax-io/matrix-synapse-rest-auth/blob/master/README.md) 
as authentication module

# Support
## Community
If you need help, want to report a bug or just say hi, you can reach us on Matrix at 
[#mxisd:kamax.io](https://matrix.to/#/#mxisd:kamax.io) or [directly peek anonymously](https://view.matrix.org/room/!NPRUEisLjcaMtHIzDr:kamax.io/).
For more high-level discussion about the Identity Server architecture/API, go to 
[#matrix-identity:matrix.org](https://matrix.to/#/#matrix-identity:matrix.org)

## Professional
If you would prefer professional support/custom development for mxisd and/or for Matrix in general, including other open source technologies/products, 
please visit [our website](https://www.kamax.io/) to get in touch with us and get a quote.
