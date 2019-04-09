# Synapse Identity Store
Synapse's Database itself can be used as an Identity store. This identity store is a regular SQL store with
built-in default queries that matches Synapse DB.

## Features
|                       Name                      | Supported |
|-------------------------------------------------|-----------|
| [Authentication](../features/authentication.md) | No        |
| [Directory](../features/directory.md)           | Yes       |
| [Identity](../features/identity.md)             | Yes       |
| [Profile](../features/profile.md)               | Yes       |

- Authentication is done by Synapse itself.
- Roles are mapped to communities. The Role name/ID uses the community ID in the form `+id:domain.tld`

## Configuration
### Basic
```yaml
synapseSql:
  enabled: <boolean>
```
Enable/disable the identity store

---

```yaml
synapseSql:
  type: <string>
```
Set the SQL backend to use which is configured in synapse:
- `sqlite`
- `postgresql`

### SQLite
```yaml
synapseSql:
  connection: <string>
```
Set the value to the absolute path to the Synapse SQLite DB file.
Example: `/path/to/synapse/sqliteFile.db`

### PostgreSQL
```yaml
synapseSql:
  connection: //<HOST[:PORT]/DB?user=USER&password=PASS
```
Set the connection info for the database by replacing the following values:
- `HOST`: Hostname of the SQL server
- `PORT`: Optional port value, if not default
- `DB`: Database name
- `USER`: Username for the connection
- `PASS`: Password for the connection

### Query customization
See the [SQL Identity store](sql.md)
