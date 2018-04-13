# Synapse Identity Store
Synapse's Database itself can be used as an Identity store.

## Features
|      Name      | Supported? |
|----------------|------------|
| Authentication | No         |
| Directory      | Yes        |
| Identity       | Yes        |

Authentication is done by Synapse itself.

## Configuration
### Basic
```yaml
synapseSql.enabled: <boolean>
```
Enable/disable the identity store

---

```yaml
synapseSql.type: <string>
```
Set the SQL backend to use which is configured in synapse:
- `sqlite`
- `postgresql`

### SQLite
```yaml
synapseSql.connection: <string>
```
Set the value to the absolute path to the Synapse SQLite DB file.
Example: `/path/to/synapse/sqliteFile.db`

### PostgreSQL
```yaml
synapseSql.connection: //<HOST[:PORT]/DB?username=USER&password=PASS
```
Set the connection info for the database by replacing the following values:
- `HOST`: Hostname of the SQL server
- `PORT`: Optional port value, if not default
- `DB`: Database name
- `USER`: Username for the connection
- `PASS`: Password for the connection

### Query customization
See the [SQL Identity store](sql.md)
