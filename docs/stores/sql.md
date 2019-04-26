# SQL Identity store
## Supported Databases
- PostgreSQL
- MariaDB
- MySQL
- SQLite

## Features
|                       Name                      | Supported |
|-------------------------------------------------|-----------|
| [Authentication](../features/authentication.md) | No        |
| [Directory](../features/directory.md)           | Yes       |
| [Identity](../features/identity.md)             | Yes       |
| [Profile](../features/profile.md)               | Yes       |

Due to the implementation complexity of supporting arbitrary hashing/encoding mechanisms or auth flow, Authentication
will be out of scope of SQL Identity stores and should be done via one of the other identity stores, typically
the [Exec Identity Store](exec.md) or the [REST Identity Store](rest.md).

## Configuration
### Basic
```yaml
sql:
  enabled: <boolean>
```
Enable/disable the identity store

---

```yaml
sql:
  type: <string>
```
Set the SQL backend to use:
- `sqlite`
- `postgresql`
- `mariadb`
- `mysql`

### Connection
#### SQLite
```yaml
sql:
  connection: <string>
```
Set the value to the absolute path to the Synapse SQLite DB file.
Example: `/path/to/sqlite/file.db`

#### Others
```yaml
sql:
  connection: //<HOST[:PORT]/DB?user=USER&password=PASS
```
Set the connection info for the database by replacing the following values:
- `HOST`: Hostname of the SQL server
- `PORT`: Optional port value, if not default
- `DB`: Database name
- `USER`: Username for the connection
- `PASS`: Password for the connection

This follow the JDBC URI syntax. See [official website](https://docs.oracle.com/javase/tutorial/jdbc/basics/connecting.html#db_connection_url).

### Directory
```yaml
sql:
  directory:
    enabled: false
```

---

```yaml
sql:
  directory:
    query:
      name:
        type: <string>
        value: <string>
      threepid:
        type: <string>
        value: <string>
```
For each query, `type` can be used to tell mxisd how to process the ID column:
- `localpart` will append the `matrix.domain` to it
- `mxid` will use the ID as-is. If it is not a valid Matrix ID, the search will fail.

`value` is the SQL query and must return two columns:
- The first being the User ID
- The second being its display name

Example:
```yaml
sql:
  directory:
    query:
      name:
        type: 'localpart'
        value: 'SELECT idColumn, displayNameColumn FROM table WHERE displayNameColumn LIKE ?'
      threepid:
        type: 'localpart'
        value: 'SELECT idColumn, displayNameColumn FROM table WHERE threepidColumn LIKE ?'
```

### Identity
**NOTE**: Only single lookup is supported. Bulk lookup always returns no mapping. This is a restriction as the Matrix API
does not allow paging or otherwise limit of results of the API, potentially leading to thousands and thousands 3PIDs at once.

```yaml
sql:
  identity:
    enabled: <boolean>
    type: <string>
    query: <string>
    medium:
     mediumTypeExample: <dedicated query>
```
`type` is used to tell mxisd how to process the returned `uid` column containing the User ID:
- `localpart` will build a full Matrix ID using the `matrix.domain` value.
- `mxid` will use the ID as-is. If it is not a valid Matrix ID, lookup(s) will fail.

A specific query can also given per 3PID medium type.

### Profile
```yaml
sql:
  profile:
    enabled: <boolean>
    displayName:
      query: <string>
    threepid:
      query: <string>
    role:
      type: <string>
      query: <string>
    

```
For the `role` query, `type` can be used to tell mxisd how to inject the User ID in the query:
- `localpart` will extract and set only the localpart.
- `mxid` will use the ID as-is.

On each query, the first parameter `?` is set as a string with the corresponding ID format.
