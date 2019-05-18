# Notifications: Template generator
Most of the Identity actions will trigger a notification of some kind, informing the user of some confirmation, next step
or just informing them about the current state of things.

Those notifications are by default generated from templates and by replacing placeholder tokens in them with the relevant
values of the notification. It is possible to customize the value of some placeholders, making easy to set values in the builtin templates, and/or
provide your own custom templates.

Templates for the following events/actions are available:
- [3PID invite](../../features/identity.md)
- [3PID session: validation](../session/session.md)
- [3PID session: fraudulent unbind](https://github.com/kamax-matrix/mxisd/wiki/mxisd-and-your-privacy#improving-your-privacy-one-commit-at-the-time)
- [Matrix ID invite](../../features/experimental/application-service.md#email-notification-about-room-invites-by-matrix-ids)

## Placeholders
All placeholders **MUST** be surrounded with `%` in the template. Per example, the `DOMAIN` placeholder would become
`%DOMAIN%` within the template. This ensures replacement doesn't happen on non-placeholder strings.

### Global
The following placeholders are available in every template:

| Placeholder                     | Purpose                                                                      |
|---------------------------------|------------------------------------------------------------------------------|
| `DOMAIN`                        | Identity server authoritative domain, as configured in `matrix.domain`       |
| `DOMAIN_PRETTY`                 | Same as `DOMAIN` with the first letter upper case and all other lower case   |
| `FROM_EMAIL`                    | Email address configured in `threepid.medium.<3PID medium>.identity.from`    |
| `FROM_NAME`                     | Name configured in `threepid.medium.<3PID medium>.identity.name`             |
| `RECIPIENT_MEDIUM`              | The 3PID medium, like `email` or `msisdn`                                    |
| `RECIPIENT_MEDIUM_URL_ENCODED`  | URL encoded value of `RECIPIENT_MEDIUM`                                      |
| `RECIPIENT_ADDRESS`             | The address to which the notification is sent                                |
| `RECIPIENT_ADDRESS_URL_ENCODED` | URL encoded value of `RECIPIENT_ADDRESS`                                     |

### Room invitation
Specific placeholders:

| Placeholder                  | Purpose                                                                           |
|------------------------------|-----------------------------------------------------------------------------------|
| `SENDER_ID`                  | Matrix ID of the user who made the invite                                         |
| `SENDER_NAME`                | Display name of the user who made the invite, if not available/set, empty         |
| `SENDER_NAME_OR_ID`          | Display name of the user who made the invite. If not available/set, its Matrix ID |
| `INVITE_MEDIUM`              | The 3PID medium for the invite.                                                   |
| `INVITE_MEDIUM_URL_ENCODED`  | URL encoded value of `INVITE_MEDIUM`                                              |
| `INVITE_ADDRESS`             | The 3PID address for the invite.                                                  |
| `INVITE_ADDRESS_URL_ENCODED` | URL encoded value of `INVITE_ADDRESS`                                             |
| `ROOM_ID`                    | The Matrix ID of the Room in which the invite took place                          |
| `ROOM_NAME`                  | The Name of the room in which the invite took place. If not available/set, empty  |
| `ROOM_NAME_OR_ID`            | The Name of the room in which the invite took place. If not available/set, its ID |
| `REGISTER_URL`               | The URL to provide to the user allowing them to register their account, if needed |

### Validation of 3PID Session
Specific placeholders:

| Placeholder        | Purpose                                                                              |
|--------------------|--------------------------------------------------------------------------------------|
| `VALIDATION_LINK`  | URL, including token, to validate the 3PID session.                                  |
| `VALIDATION_TOKEN` | The token needed to validate the session, in case the user cannot use the link.      |
| `NEXT_URL`         | URL to redirect to after the sessions has been validated.                            |

## Templates
mxisd comes with a set of builtin templates to easily get started. Those templates can be found
[in the repository](https://github.com/kamax-matrix/mxisd/tree/master/src/main/resources/threepids). If you want to use
customized templates, we recommend using the builtin templates as a starting point.

> **NOTE**: The link above point to the latest version of the built-in templates. Those might be different from your
version. Be sure to view the repo at the current tag.

## Configuration
All configuration is specific to [3PID mediums](https://matrix.org/docs/spec/appendices.html#pid-types) and happen
under the namespace `threepid.medium.<medium>.generators.template`.

Under such namespace, the following keys are available:
- `invite`: Path to the 3PID invite notification template
- `session.validation`: Path to the 3PID session validation notification template
- `session.unbind.fraudulent`: Path to the 3PID session fraudulent unbind notification template
- `generic.matrixId`: Path to the Matrix ID invite notification template
- `placeholder`: Map of key/values to set static values for some placeholders.

The `placeholder` map supports the following keys, mapped to their respective template placeholders:
- `REGISTER_URL`

### Example
#### Simple
```yaml
threepid:
  medium:
    email:
      generators:
        template:
          placeholder:
            REGISTER_URL: 'https://matrix-client.example.org'
```
In this configuration, the builtin templates are used and a static value for the `REGISTER_URL` is set, allowing to point
a newly invited user to a webapp allowing the creation of its account on the server.

#### Advanced
To configure paths to the various templates:
```yaml
threepid:
  medium:
    email:
      generators:
        template:
          invite: '/path/to/invite-template.eml'
          session:
            validation: '/path/to/validate-template.eml'
            unbind:
              fraudulent: '/path/to/unbind-fraudulent-template.eml'
          generic:
            matrixId: '/path/to/mxid-invite-template.eml'
          placeholder:
            REGISTER_URL: 'https://matrix-client.example.org'
```
In this configuration, a custom template is used for each event and a static value for the `REGISTER_URL` is set.
