# Operations Guide
- [Overview](#overview)
- [Maintenance](#maintenance)
- [Backup](#backup)

## Overview
This document gives various information for the day-to-day management and operations of mxisd.

## Maintenance
mxisd does not require any maintenance task to run at optimal performance.

## Backup
### Run
mxisd requires all file in its configuration and data directory to be backed up.  
They are usually located at:
- `/etc/mxisd`
- `/var/lib/mxisd`

### Restore
Reinstall mxisd, restore the two folders above in the appropriate location (depending on your install method) and you
will be good to go. Simply start mxisd to restore functionality.
