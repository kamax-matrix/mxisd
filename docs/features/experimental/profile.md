# Profile enhancement
**WARNING**: Alpha feature, not officially supported. Do not use.

This feature allows to enhance a profile query with more info than just Matrix ID and Display name, allowing for custom
applications to retrieve custom data not currently provided by synapse, per example.

## Configuration
### Reverse proxy
#### Apache
```apache
ProxyPassMatch "^/_matrix/client/r0/profile/([^/]+)$" "http://127.0.0.1:8090/_matrix/client/r0/profile/$1"
```
