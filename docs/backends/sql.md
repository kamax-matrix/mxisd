# SQL Backend
## Configuration
To be completed. For now, see default structure and values:
```
sql:
  enabled: false
  type: 'sqlite' or 'postgresql'
  connection: ''
  auth:
    enabled: false
  directory:
    enabled: false
    query:
      name:
        type: 'localpart'
        value: 'SELECT 1'
      threepid:
        type: 'localpart'
        value: 'SELECT 1'
  identity:
    type: 'localpart'
    query: 'SELECT user_id AS uid FROM user_threepids WHERE medium = ? AND address = ?'
```
