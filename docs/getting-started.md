# Getting started
1. [Preparation](#preparation)
2. [Install](#install)
3. [Configure](#configure)
4. [Integrate](#integrate)
5. [Validate](#validate)
6. [Next steps](#next-steps)

Following these quick start instructions, you will have a basic setup that can perform recursive/federated lookups.  
This will be a good ground work for further integration with features and your existing Identity stores.

---

If you would like a more fully integrated setup out of the box, the [matrix-docker-ansible-deploy](https://github.com/spantaleev/matrix-docker-ansible-deploy)
project provides a turn-key full-stack solution, including LDAP and the various mxisd features enabled and ready.  
We work closely with the project owner so the latest mxisd version is always supported.

If you choose to use it, this Getting Started guide is not applicable - See the project documentation. You may then
directly go to the [Next steps](#next-steps).

## Preparation
You will need:
- Working Homeserver, ideally with working federation
- Reverse proxy with regular TLS/SSL certificate (Let's encrypt) for your mxisd domain

If you use synapse:
- It requires an HTTPS connection when talking to an Identity service, **a reverse proxy is required** as mxisd does
  not support HTTPS listener at this time.
- HTTPS is hardcoded when talking to the Identity server. If your Identity server URL in your client is `https://matrix.example.org/`,
  then you need to ensure `https://matrix.example.org/_matrix/identity/api/v1/...` will reach mxisd if called from the synapse host.
  In doubt, test with `curl` or similar. 

For maximum integration, it is best to have your Homeserver and mxisd reachable via the same public hostname.

Be aware of a [NAT/Reverse proxy gotcha](https://github.com/kamax-matrix/mxisd/wiki/Gotchas#nating) if you use the same
host.

The following Quick Start guide assumes you will host the Homeserver and mxisd under the same hostname.  
If you would like a high-level view of the infrastructure and how each feature is integrated, see the
[dedicated document](architecture.md)

## Install
Install via:
- [Docker image](install/docker.md)
- [Debian package](install/debian.md)
- [ArchLinux](install/archlinux.md)
- [NixOS](install/nixos.md)
- [Sources](build.md)

See the [Latest release](https://github.com/kamax-matrix/mxisd/releases/latest) for links to each.

## Configure
> **NOTE**: Please view the install instruction for your platform, as this step might be optional or already handled for you.
  
> **NOTE**: Details about configuration syntax and format are described [here](configure.md)

If you haven't created a configuration file yet, copy `mxisd.example.yaml` to where the configuration file is stored given
your installation method and edit to your needs.

The following items must be at least configured:
- `matrix.domain` should be set to your Homeserver domain (`server_name` in synapse configuration)
- `key.path` will store the signing keys, which must be kept safe! If the file does not exist, keys will be generated for you.
- `storage.provider.sqlite.database` is the location of the SQLite Database file which will hold state (invites, etc.)

If your HS/mxisd hostname is not the same as your Matrix domain, configure `server.name`.  
Complete configuration guide is available [here](configure.md).

## Integrate
For an overview of a typical mxisd infrastructure, see the [dedicated document](architecture.md)
### Reverse proxy
#### Apache2
In the `VirtualHost` section handling the domain with SSL, add the following and replace `0.0.0.0` by the internal
hostname/IP pointing to mxisd.  
**This line MUST be present before the one for the homeserver!**
```apache
ProxyPass /_matrix/identity http://0.0.0.0:8090/_matrix/identity
```

Typical configuration would look like:
```apache
<VirtualHost *:443>
    ServerName matrix.example.org
    
    # ...
    
    ProxyPreserveHost on
    ProxyPass /_matrix/identity http://localhost:8090/_matrix/identity
    ProxyPass /_matrix http://localhost:8008/_matrix
</VirtualHost>
```

#### nginx
In the `server` section handling the domain with SSL, add the following and replace `0.0.0.0` with the internal
hostname/IP pointing to mxisd.
**This line MUST be present before the one for the homeserver!**
```nginx
location /_matrix/identity {
    proxy_pass http://0.0.0.0:8090/_matrix/identity;
}
```

Typical configuration would look like:
```nginx
server {
    listen 443 ssl;
    server_name matrix.example.org;
    
    # ...
    
    location /_matrix/identity {
        proxy_pass http://localhost:8090/_matrix/identity;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
    }
    
   
    location ~ ^(/_matrix|/_synapse/client) {
        # note: do not add a path (even a single /) after the port in `proxy_pass`,
        # otherwise nginx will canonicalise the URI and cause signature verification
        # errors.
        proxy_pass http://localhost:8008;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Host $host;

        # Nginx by default only allows file uploads up to 1M in size
        # Increase client_max_body_size to match max_upload_size defined in homeserver.yaml
        client_max_body_size 50M;
    }
}

```

### Synapse
Add your mxisd domain into the `homeserver.yaml` at `trusted_third_party_id_servers` and restart synapse.  
In a typical configuration, you would end up with something similar to:
```yaml
trusted_third_party_id_servers:
    - matrix.example.org
```
It is **highly recommended** to remove `matrix.org` and `vector.im` (or any other default entry) from your configuration
so only your own Identity server is authoritative for your HS.

## Validate
Find someone to invite by email that has a published 3PID mapping.
  
If it worked, it means you are up and running and can enjoy mxisd in its basic mode! Congratulations!  
If it did not work, read the basic [troubleshooting guide](troubleshooting.md). Given that this product is no longer
supported as a standalone product, no support is available for the community.

## Next steps
Once your mxisd server is up and running, there are several ways you can enhance and integrate further with your
infrastructure:

- [Enable extra features](features/)
- [Use your own Identity stores](stores/README.md)
- [Hardening your mxisd installation](install/security.md)
- [Learn about day-to-day operations](operations.md)
