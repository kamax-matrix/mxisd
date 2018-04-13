# Profile enhancement
**WARNING**: Alpha feature not officially supported. Do not use.

## Configuration
### Reverse proxy
#### Apache
```apache
ProxyPassMatch "^/_matrix/client/r0/profile/([^/]+)$" "http://127.0.0.1:8090/_matrix/client/r0/profile/$1"
```
