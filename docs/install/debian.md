# Debian package
## Install
1. Donwload the [latest release](https://github.com/kamax-io/mxisd/releases/latest)
2. Run:
```
dpkg -i /path/to/downloaded/mxisd.deb
```
## Files
| Location                          | Purpose                                      |
|-----------------------------------|----------------------------------------------|
| /etc/mxisd                        | Configuration directory                      |
| /etc/mxisd/mxisd.yaml             | Main configuration file                      |
| /etc/mxisd/signing.key            | Default location for mxisd signing keys      |
| /etc/systemd/system/mxisd.service | Systemd configuration file for mxisd service |
| /usr/lib/mxisd                    | Binairies                                    |
| /var/lib/mxisd                    | Data                                         |

## Control
Start mxisd using:
```
sudo systemctl start mxisd
```

Stop mxisd using:
```
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
journalctl -f n 99 -u mxisd
```
