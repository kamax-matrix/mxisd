# Configuration
- [Concepts](#concepts)
  - [Syntax](#syntax)
  - [Variables](#variables)
- [Matrix](#matrix)
- [Server](#server)
- [Storage](#storage)
- [Identity stores](#identity-stores)
- [3PID Validation sessions](#3pid-validation-sessions)
- [Notifications](#notifications)

## Concepts
### Syntax
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

### Variables
It is possible to copy the value of a configuration item into another using the syntax `${config.key.item}`.  
Example that will copy the value of `matrix.domain` into `server.name`:
```
matrix:
  domain: 'example.org'

server:
  name: '${matrix.domain}'
```

**WARNING:** mxisd might overwrite/adapt some values during launch. Those changes will not be reflected into copied keys.


## Matrix
`matrix.domain`
Matrix domain name, same as the Homeserver, used to build appropriate Matrix IDs |

---

`matrix.identity.servers`
Namespace to create arbitrary list of Identity servers, usable in other parts of the configuration |

Example:
```
matrix.identity.servers:
  myOtherServers:
    - 'https://other1.example.org'
    - 'https://other2.example.org'
```
Create a list under the label `root` containing a single Identity server, `https://matrix.org`

## Server
- `server.name`: Public hostname of mxisd, if different from the Matrix domain.
- `server.port`: HTTP port to listen on (unencrypted)
- `server.publicUrl`: Defaults to `https://${server.name}`

## Storage
### SQLite
`storage.provider.sqlite.database`: Absolute location of the SQLite database

## Identity stores
See the [Identity stores](backends/README.md) for specific configuration

## 3PID Validation sessions
See the dedicated documents:
- [Flow](sessions/3pid.md)
- [Branding](sessions/3pid-views.md)

## Notifications
- `notification.handler.<3PID medium>`: Handler to use for the given 3PID medium. Repeatable.

Example:
```yaml
notification.handler.email: 'sendgrid'
notification.handler.msisdn: 'raw'
```
- Emails notifications would use the `sendgrid` handler, which define its own configuration under `notification.handlers.sendgrid`
- Phone notification would use the `raw` handler, basic default built-in handler of mxisd

### Handlers
- `notification.handers.<handler ID>`: Handler-specific configuration for the given handler ID. Repeatable.

Example:
```yaml
notification.handlers.raw: ...
notification.handlers.sendgrid: ...
```

Built-in:
- [Raw](threepids/notifications/basic-handler.md)
- [SendGrid](threepids/notifications/sendgrid-handler.md)
