# Install from sources

## Instructions
Follow the [build instructions](../build.md) then:

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

## Next steps
- [Integrate with your infrastructure](getting-started.md#integrate)