# Install from sources
## Instructions
Follow the [build instructions](../build.md) then:

### Prepare files and directories:
```bash
# Create a dedicated user
useradd -r mxisd

# Create config directory and set ownership
mkdir -p /etc/mxisd

# Create data directory and set ownership
mkdir -p /var/lib/mxisd
chown -R mxisd /var/lib/mxisd

# Create bin directory, copy the jar and launch scriot to bin directory
mkdir /usr/lib/mxisd
cp ./build/libs/mxisd.jar /usr/lib/mxisd/
cp ./src/script/mxisd /usr/lib/mxisd
chown -R mxisd /usr/lib/mxisd
chmod a+x /usr/lib/mxisd/mxisd

# Create symlink for easy exec
ln -s /usr/lib/mxisd/mxisd /usr/bin/mxisd
```

### Prepare config file
Copy the sample config file `./mxisd.example.yaml` to `/etc/mxisd/mxisd.yaml`, edit to your needs

### Prepare Systemd
1. Copy `src/systemd/mxisd.service` to `/etc/systemd/system/` and edit if needed
2. Enable service for auto-startup
```bash
systemctl enable mxisd
```

### Run
```bash
systemctl start mxisd
```

## Debug
mxisd logs to stdout, which is normally sent to `/var/log/syslog` or `/var/log/messages`.
