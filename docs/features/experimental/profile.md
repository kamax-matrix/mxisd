# Profile
**WARNING**: The following sub-features are considered experimental and not officially supported. Use at your own peril.

## Public Profile enhancement
This feature allows to enhance a public profile query with more info than just Matrix ID and Display name, allowing for
custom applications to retrieve custom data not currently provided by synapse, per example.

**WARNING**: This information can be queried without authentication as per the specification. Do not enable unless in a
controlled environment.

### Configuration
#### Reverse proxy
##### Apache
```apache
ProxyPassMatch "^/_matrix/client/r0/profile/([^/]+)$" "http://127.0.0.1:8090/_matrix/client/r0/profile/$1"
```
