# From source
- [Binaries](#binaries)
  - [Requirements](#requirements)
  - [Build](#build)
- [Debian package](#debian-package)
- [Docker image](#docker-image)
- [Next steps](#next-steps)

## Binaries
### Requirements
- JDK 1.8

### Build
```bash
git clone https://github.com/kamax-matrix/mxisd.git
cd mxisd
./gradlew build
```

Create a new configuration file by coping `application.example.yaml` to `application.yaml` and edit to your needs.  
For advanced configuration, see the [Configure section](configure.md).  
**NOTE**: `application.yaml` is also called `mxisd.yaml` in some specific installations.

Start the server in foreground to validate the build and configuration:
```bash
java -jar build/libs/mxisd.jar
```

Ensure the signing key is available:
```bash
$ curl 'http://localhost:8090/_matrix/identity/api/v1/pubkey/ed25519:0'
{"public_key":"..."}
```

Test basic recursive lookup (requires Internet connection with access to TCP 443):
```bash
$ curl 'http://localhost:8090/_matrix/identity/api/v1/lookup?medium=email&address=mxisd-federation-test@kamax.io'
{"address":"mxisd-federation-test@kamax.io","medium":"email","mxid":"@mxisd-lookup-test:kamax.io",...}
```

If you enabled LDAP, you can also validate your config with a similar request after replacing the `address` value with
something present within your LDAP
```bash
curl 'http://localhost:8090/_matrix/identity/api/v1/lookup?medium=email&address=john.doe@example.org'
```

If you plan on testing the integration with a homeserver, you will need to run an HTTPS reverse proxy in front of it
as the reference Home Server implementation [synapse](https://github.com/matrix-org/synapse) requires a HTTPS connection
to an ID server.  

Next step: [Install your compiled binaries](install/source.md)

## Debian package
Requirements:
- fakeroot
- dpkg-deb

[Build mxisd](#build) then:
```bash
./gradlew buildDeb 
```
You will find the debian package in `build/dist`.  
Then follow the instruction in the [Debian package](install/debian.md) document.

## Docker image
[Build mxisd](#build) then:
```bash
./gradlew dockerBuild
```
Then follow the instructions in the [Docker install](install/docker.md#configure) document.

## Next steps
- [Integrate with your infrastructure](getting-started.md#integrate)