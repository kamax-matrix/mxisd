# Docker
## Fetch
Pull the latest stable image:
```
docker pull kamax/mxisd
```

## Run
Run it (adapt volume paths to your host):
```
docker run --rm -v /data/mxisd/etc:/etc/mxisd -v /data/mxisd/var:/var/mxisd -p 8090:8090 -t kamax/mxisd
```

For more info, including the list of possible tags, see [the public repository](https://hub.docker.com/r/kamax/mxisd/)
