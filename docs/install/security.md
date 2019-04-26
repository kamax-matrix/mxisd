# Security hardening
## Overview
This document outlines the various operations you may want to perform to increase the security of your installation and
avoid leak of credentials/key pairs

## Configuration
Your config file should have the following ownership:
- Dedicated user for mxisd, used to run the software
- Dedicated group for mxisd, used by other applications to access and read configuration files

Your config file should have the following access:
- Read and write for the mxisd user
- Read for the mxisd group
- Nothing for others

This translates into `640` and be applied with `chmod 640 /path/to/config/file.yaml`.

## Data
The only sensible place is the key store where mxisd's signing keys are stored. You should therefore limit access to only
the mxisd user, and deny access to anything else.

Your key store should have the following access:
- Read and write for the mxisd user
- Nothing for the mxisd group
- Nothing for others

The identity store can either be a file or a directory, depending on your version. v1.4 and higher are using a directory,
everything before is using a file.
- If your version is directory-based, you will want to apply chmod `700` on it.
- If your version is file-based, you will want to apply chmod `600` on it.
