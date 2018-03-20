# Profile enhancement

## Configuration

### Reverse proxy

#### Apache
```
ProxyPassMatch "^/_matrix/client/r0/profile/([^/]+)$" "http://127.0.0.1:8090/_matrix/client/r0/profile/$1"
ProxyPassMatch "^/_matrix/client/r0/profile/([^/]+)/(.+)" "http://127.0.0.1:8008/_matrix/client/r0/profile/$1/$2"

```
