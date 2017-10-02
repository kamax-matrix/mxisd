# Configuration
- [Syntax](#syntax)
- [Variables](#variables)
- [Categories](#categories)

## Syntax
The configuration file is YAML based, allowing two types of syntax.

Properties-like:
```
my.config.item: 'value'
```

Object-like:
```
my:
  config:
    item: 'value'

```
These can also be combined within the same file.  
Both syntax will be used interchangeably in these documents.

Default values for each possible option are documented [here](../src/main/resources/application.yaml)

## Variables
It is possible to copy the value of a configuration item into another using the syntax `${config.key.item}`.  
Example that will copy the value of `matrix.domain` into `server.name`:
```
matrix:
  domain: 'example.org'

server:
  name: '${matrix.domain}'
```

**WARNING:** mxisd might overwrite/adapt some values during launch. Those changes will not be reflected into copied keys.

## Categories
For each category below, the base configuration path will be given, which needs to be appened to every configuration
item described.

Example: if the base path was `basePath` and the following table was given:

| Name | Purpose |
|------|---------|
| item1 | To give an example |
| item2 | To give another example |

The following configurations could be used, all being equivalent:
```
basePath.item1: 'myValue'
basePath.item2: 'myOtherValue'
```
```
basePath:
  item1: 'myValue'
  item2: 'myOtherValue'
```
```
basePath.item1: 'myValue'
basePath:
  item2: 'myOtherValue'
```

---

In case a relative base path is given, it is appended to the one above.

Example: With base path `basePath`, the relative base `relativeBasePath` and the following table:
  
| Name | Purpose |
|------|---------|
| item1 | To give an example |
| item2 | To give another example |

The following configurations could be used, all being equivalent:
```
basePath.relativeBasePath.item1: 'myValue'
basePath.relativeBasePath.item2: 'myOtherValue'
```
```
basePath:
  relativeBasePath:
    item1: 'myValue'
    item2: 'myOtherValue'
```
```
basePath.relativeBasePath.item1: 'myValue'
basePath:
  relativeBasePath:
    item2: 'myOtherValue'
```

### Matrix
Base path: `matrix`

| Name | Purpose |
|------|---------|
| `domain` | Matrix domain name, same as the Homeserver, used to build appropriate Matrix IDs |

---

Relative base path: `identity`

| Name | Purpose |
|------|---------|
| `servers` | Namespace to create arbitrary list of Identity servers, usable in other parts of the configuration |

Example:
```
matrix.identity.servers:
  root:
    - 'https://matrix.org'
```
Create a list under the label `root` containing a single Identity server, `https://matrix.org`
### Server
| Name | Purpose |
|------|---------|
| `name` | Public hostname of mxisd, if different from the Matrix domain |
| `port` | HTTP port to listen on (unencrypted) |
| `publicUrl` | Defaults to `https://${server.name}` |

### Storage
Base path: `storage`

| Name | Purpose |
|------|---------|
| `backend` | Specify which SQL backend to use. only `sqlite` is currently supported. |

---
Relative base path: `provider.sqlite`

| Name | Purpose |
|------|---------|
| `database` | Absolute location of the SQLite database |

### Backends
- [LDAP](backends/ldap.md)
- [SQL](backends/sql.md)
- [REST](backends/rest.md)
- [Google Firebase](backends/firebase.md)

### Lookups
work in progress, should not be configured.

### Sessions
See the [dedicated document](sessions/3pid.md)

### Notifications
Base path: `notification`

| Name | Purpose |
|------|---------|
| handler | Namespace to specify the handler to use for each 3PID |
| handlers | Namespace used by individual handlers for their own configuration |

Example:
```
notification:
  handler:
    email: 'sendgrid'
    msisdn: 'raw'
  handlers:
    raw:
      ...
    sendgrid:
      ...
```
- Emails notifications would use the `sendgrid` handler, which define its own configuration user `handlers.sendgrid`
- Phone notification would use the `raw` handler, basic default built-in handler of mxisd
#### Handlers
Relative base path: `handlers`

Built-in:
- [Basic](threepids/notifications/basic-handler.md)
- [SendGrid](threepids/notifications/sendgrid-handler.md)

### Views
See the [dedicated document](sessions/3pid-views.md)

### DNS Overwite
Specific to other features.
