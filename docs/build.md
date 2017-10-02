# From source
- [Binaries](#binaries)
- [Debian package](#debian-package)
- [Docker image](#docker-image)

## Binaires
### Requirements
- JDK 1.8

### Build
```
git clone https://github.com/kamax-io/mxisd.git
cd mxisd
./gradlew build
```

Create a new configuration file by coping `application.example.yaml` to `application.yaml` and edit to your needs.  
For advanced configuration, see the [Configure section](configure.md).

Start the server in foreground to validate the build and configuration:
```
java -jar build/libs/mxisd.jar
```

Ensure the signing key is available:
```
$ curl 'http://localhost:8090/_matrix/identity/api/v1/pubkey/ed25519:0'
{"public_key":"..."}
```

Test basic recursive lookup (requires Internet connection with access to TCP 443):
```
$ curl 'http://localhost:8090/_matrix/identity/api/v1/lookup?medium=email&address=mxisd-lookup-test@kamax.io'
{"address":"mxisd-lookup-test@kamax.io","medium":"email","mxid":"@mxisd-lookup-test:kamax.io",...}
```

If you enabled LDAP, you can also validate your config with a similar request after replacing the `address` value with
something present within your LDAP
```
curl 'http://localhost:8090/_matrix/identity/api/v1/lookup?medium=email&address=john.doe@example.org'
```

If you plan on testing the integration with a homeserver, you will need to run an HTTPS reverse proxy in front of it
as the reference Home Server implementation [synapse](https://github.com/matrix-org/synapse) requires a HTTPS connection
to an ID server.  
See the [Integration section](#integration) for more details.

### Install
1. Prepare files and directories:
```
# Create a dedicated user
useradd -r mxisd

# Create bin directory
mkdir /opt/mxisd

# Create config directory and set ownership
mkdir /etc/opt/mxisd
chown mxisd /etc/opt/mxisd

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

2. Copy the sample config file `./application.example.yaml` to `/etc/opt/mxisd/mxisd.yaml`, edit to your needs
4. Copy `<repo root>/src/systemd/mxisd.service` to `/etc/systemd/system/` and edit if needed
5. Enable service for auto-startup
```
systemctl enable mxisd
```
6. Start mxisd
```
systemctl start mxisd
```

## Debian package
### Requirements
- fakeroot
- dpkg-deb

### Build
[Build mxisd](#build) then:
```
./gradlew buildDeb 
```
You will find the debian package in `build/dist`

## Docker image
[Build mxisd](#build) then:
```
./gradlew dockerBuild
```
You can run a container of the given image and test it with the following command (adapt volumes host paths):
```
docker run -v /data/mxisd/etc:/etc/mxisd -v /data/mxisd/var:/var/mxisd -p 8090:8090 -t kamax/mxisd:latest-dev
```
