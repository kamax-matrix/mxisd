# SQL Identity stores
## Supported Databases
- PostgreSQL
- MariaDB
- MySQL
- SQLite

## Features
|      Name      | Supported? |
|----------------|------------|
| Authentication | No         |
| Directory      | Yes        |
| Identity       | Yes        |

Due to the implementation complexity of supporting arbitrary hashing/encoding mechanisms or auth flow, Authentication
will be out of scope of SQL Identity stores and should be done via one of the other identity stores, typically
the [REST Identity store](rest.md).

## Configuration
### Basic
```yaml
sql.enabled: <boolean>
```
Enable/disable the identity store

---

```yaml
sql.type: <string>
```
Set the SQL backend to use:
- `sqlite`
- `postgresql`
- `mariadb`
- `mysql`

### Connection
#### SQLite
```yaml
sql.connection: <string>
```
Set the value to the absolute path to the Synapse SQLite DB file.
Example: `/path/to/sqlite/file.db`

#### Others
```yaml
sql.connection: //<HOST[:PORT]/DB?username=USER&password=PASS
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
sql.directory.enabled: false
```

```yaml
sql.directory.query:
  name:
    type: <string>
    value: <string>
  threepid:
    type: <string>
    value: <string>
```

### Identity
```yaml
sql.identity.type: <string>
sql.identity.query: <string>
```
