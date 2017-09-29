- Only work for LDAP and SQL
  - For LDAP: Set LDAP config (new global filter, optional: threepids attributes if the default identity queries were changed)
  - For SQL: Use `synapseSql` module with `type: {sqlite|postgresql}` and `database` as JDBC url after `jdbc:driver:`
    - `/path/to/db` for `sqlite`
    - `//host/db?username...` for `postgresql`)
- Configure DNS overwrite for domain name (and mention ${matrix.domain} can be used)
```
dns.overwrite.homeserver.client:
  - name: 'example.org'
    value: 'http://localhost:8008'
```
- Configure reverse proxy
  - for `/_matrix/client/r0/user_directory/search` to `http://internalIp:8008/_matrix/client/r0/user_directory/search`
  - With `ProxyPreserveHost on` on apache
