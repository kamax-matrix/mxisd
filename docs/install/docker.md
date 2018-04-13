# Docker
## Fetch
Pull the latest stable image:
```bash
docker pull kamax/mxisd
```

## Configure
On first run, simply using `MATRIX_DOMAIN` as an environment variable will create a default config for you.  
You can also provide a configuration file named `mxisd.yaml` in the volume mapped to `/etc/mxisd` before starting your
container.

## Run
Use the following command after adapting to your needs:
- The `MATRIX_DOMAIN` environment variable to yours
- The volumes host paths

```bash
docker run --rm -e MATRIX_DOMAIN=example.org -v /data/mxisd/etc:/etc/mxisd -v /data/mxisd/var:/var/mxisd -p 8090:8090 -t kamax/mxisd
```

For more info, including the list of possible tags, see [the public repository](https://hub.docker.com/r/kamax/mxisd/)
