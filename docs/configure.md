# Configuration
- [Concepts](#concepts)
  - [Syntax](#syntax)
- [Matrix](#matrix)
- [Server](#server)
- [Storage](#storage)
- [Identity stores](#identity-stores)
- [3PID Validation sessions](#3pid-validation-sessions)
- [Notifications](#notifications)

## Concepts
### Syntax
The configuration file is [YAML](http://yaml.org/) based:
```yaml
my:
  config:
    item: 'value'

```

When referencing keys in all documents, a property-like shorthand will be used. The shorthand for the above example would be `my.config.item`

## Matrix
`matrix.domain`
Matrix domain name, same as the Homeserver, used to build appropriate Matrix IDs |

---

`matrix.identity.servers`
Namespace to create arbitrary list of Identity servers, usable in other parts of the configuration |

Example:
```yaml
matrix:
  identity:
    servers:
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
See the [Identity stores](stores/README.md) for specific configuration

## 3PID Validation sessions
See the dedicated documents:
- [Flow](threepids/session/session.md)
- [Branding](threepids/session/session-views.md)

## Notifications
- `notification.handler.<3PID medium>`: Handler to use for the given 3PID medium. Repeatable.

Example:
```yaml
notification:
  handler:
    email: 'sendgrid'
    msisdn: 'raw'
```
- Emails notifications would use the `sendgrid` handler, which define its own configuration under `notification.handlers.sendgrid`
- Phone notification would use the `raw` handler, basic default built-in handler in mxisd

### Handlers
- `notification.handers.<handler ID>`: Handler-specific configuration for the given handler ID. Repeatable.

Example:
```yaml
notification:
  handlers:
    raw: ...
    sendgrid: ...
```

Built-in:
- [Raw](threepids/notification/basic-handler.md)
- [SendGrid](threepids/notification/sendgrid-handler.md)
