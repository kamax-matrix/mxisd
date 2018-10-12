# Debian package
## Install
1. Download the [latest release](https://github.com/kamax-matrix/mxisd/releases/latest)
2. Run:
```bash
dpkg -i /path/to/downloaded/mxisd.deb
```
## Files
| Location                            | Purpose                                      |
|-------------------------------------|----------------------------------------------|
| `/etc/mxisd`                        | Configuration directory                      |
| `/etc/mxisd/mxisd.yaml`             | Main configuration file                      |
| `/etc/systemd/system/mxisd.service` | Systemd configuration file for mxisd service |
| `/usr/lib/mxisd`                    | Binaries                                     |
| `/var/lib/mxisd`                    | Data                                         |
| `/var/lib/mxisd/signing.key`        | Default location for mxisd signing keys      |

## Control
Start mxisd using:
```bash
sudo systemctl start mxisd
```

Stop mxisd using:
```bash
sudo systemctl stop mxisd
```

## Troubleshoot
All logs are sent to `STDOUT` which are saved in `/var/log/syslog` by default.  
You can:
- grep & tail using `mxisd`:
```
tail -n 99 -f /var/log/syslog | grep mxisd
```
- use Systemd's journal:
```
journalctl -f -n 99 -u mxisd
```
