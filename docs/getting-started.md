# Getting started
1. [Preparation](#preparation)
2. [Install](#install)
3. [Configure](#configure)
4. [Integrate](#integrate)
5. [Validate](#validate)
6. [Next steps](#next-steps)

Following these quick start instructions, you will have a basic setup that can perform recursive/federated lookups and
talk to the central Matrix.org Identity server.  
This will be a good ground work for further integration with features and your existing Identity stores.

## Preparation
You will need:
- Working Homeserver, ideally with working federation
- Reverse proxy with regular TLS/SSL certificate (Let's encrypt) for your mxisd domain

As synapse requires an HTTPS connection when talking to an Identity service, **a reverse proxy is required** as mxisd does
not support HTTPS listener at this time.

For maximum integration, it is best to have your Homeserver and mxisd reachable via the same hostname.

Be aware of a [NAT/Reverse proxy gotcha](https://github.com/kamax-matrix/mxisd/wiki/Gotchas#nating) if you use the same
hostname.

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
**NOTE**: please view the install instruction for your platform, as this step might be optional or already handled for you.

Create/edit a minimal configuration (see installer doc for the location):
```yaml
matrix.domain: 'example.org'
key.path: '/path/to/signing.key.file'
storage.provider.sqlite.database: '/path/to/mxisd.db'
```  
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
    ServerName example.org
    
    ...
    
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
    server_name example.org;
    
    ...
    
    location /_matrix/identity {
        proxy_pass http://localhost:8090/_matrix/identity;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
    }
    
    location /_matrix {
        proxy_pass http://localhost:8008/_matrix;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
    }
}
```

### Synapse
Add your mxisd domain into the `homeserver.yaml` at `trusted_third_party_id_servers` and restart synapse.  
In a typical configuration, you would end up with something similar to:
```yaml
trusted_third_party_id_servers:
    - example.org
```
It is recommended to remove `matrix.org` and `vector.im` (or any other default entry) from your configuration so only
your own Identity server is authoritative for your HS.

## Validate
**NOTE:** In case your homeserver has no working federation, step 5 will not happen. If step 4 took place, consider
your installation validated.

1. Log in using your Matrix client and set `https://example.org` as your Identity server URL, replacing `example.org` by
the relevant hostname which you configured in your reverse proxy.
2. Create a new empty room. All further actions will take place in this room.
3. Invite `mxisd-federation-test@kamax.io`
4. The 3PID invite should be turned into a Matrix invite to `@mxisd-lookup-test:kamax.io`.
5. The invited test user will join the room, send a congratulation message and leave.
**NOTE:** You might not see a suggestion for the e-mail address, which is normal. Still proceed with the invite.
  
If it worked, it means you are up and running and can enjoy mxisd in its basic mode! Congratulations!  
If it did not work, [get in touch](../README.md#support) and we'll do our best to get you started.

## Next steps
Once your mxisd server is up and running, there are several ways you can enhance and integrate further with your
infrastructure:

- [Enable extra features](features/)
- [Use your own Identity stores](stores/README.md)
