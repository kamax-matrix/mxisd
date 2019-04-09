# Application Service
**WARNING:** These features are currently highly experimental. They can be removed or modified without notice.  
All the features requires a Homeserver capable of connecting [Application Services](https://matrix.org/docs/spec/application_service/r0.1.0.html).

The following capabilities are provided in this features:
- [Admin commands](#admin-commands)
- [Email Notification about room invites by Matrix IDs](#email-notification-about-room-invites-by-matrix-ids)
- [Auto-reject of expired 3PID invites](#auto-reject-of-expired-3pid-invites)

## Setup
> **NOTE:** Make sure you are familiar with [configuration format and rules](../../configure.md).

Integration as an Application service is a three steps process:
1. Create the baseline mxisd configuration to allow integration.
2. Integrate with the homeserver.
3. Configure the specific capabilities, if applicable.

### Configuration
#### Variables
Under the `appsvc` namespace:

| Key                   | Type    | Required | Default | Purpose                                                        |
|-----------------------|---------|----------|---------|----------------------------------------------------------------|
| `enabled`             | boolean | No       | `true`  | Globally enable/disable the feature                            |
| `user.main`           | string  | No       | `mxisd` | Localpart for the main appservice user                         |
| `endpoint.toHS.url`   | string  | Yes      | *None*  | Base URL to the Homeserver                                     |
| `endpoint.toHS.token` | string  | Yes      | *None*  | Token to use when sending requests to the Homeserver           |
| `endpoint.toAS.url`   | string  | Yes      | *None*  | Base URL to mxisd from the Homeserver                          |
| `endpoint.toHS.token` | string  | Yes      | *None*  | Token for the Homeserver to use when sending requests to mxisd |

#### Example
```yaml
appsvc:
  endpoint:
    toHS:
      url: 'http://localhost:8008'
      token: 'ExampleTokenToHS-ChangeMe!'
    toAS:
      url: 'http://localhost:8090'
      token: 'ExampleTokenToAS-ChangeMe!'
```
### Integration
#### Synapse
Under the `appsvc.registration.synapse` namespace:

| Key    | Type   | Required | Default            | Purpose                                                                  |
|--------|--------|----------|--------------------|--------------------------------------------------------------------------|
| `id`   | string | No       | `appservice-mxisd` | The unique, user-defined ID of this application service. See spec.       |
| `file` | string | Yes      | *None*             | If defined, the synapse registration file that should be created/updated |

##### Example 
```yaml
appsvc:
  registration:
    synapse:
      file: '/etc/matrix-synapse/mxisd-appservice-registration.yaml'
```

Edit your `homeserver.yaml` and add a new entry to the appservice config file, which should look something like this:
```yaml
app_service_config_files:
  - '/etc/matrix-synapse/mxisd-appservice-registration.yaml'
  - ...
```

Restart synapse when done to register mxisd.

#### Others
See your Homeserver documentation on how to integrate.

## Capabilities
### Admin commands
#### Setup
Min config:
```yaml
appsvc:
  feature:
    admin:
      allowedRoles:
        - '+aMatrixCommunity:example.org'
        - 'SomeLdapGroup'
        - 'AnyOtherArbitraryRoleFromIdentityStores'
```

#### Use
The following steps assume:
- `matrix.domain` set to `example.org`
- `appsvc.user.main` set to `mxisd` or not set

1. Invite `@mxisd:example.org` to a new direct chat
2. Type `!help`

### Email Notification about room invites by Matrix IDs
This feature allows for users found in Identity stores to be instantly notified about Room Invites, regardless if their
account was already provisioned on the Homeserver.

#### Requirements
- [Identity store(s)](../../stores/README.md) supporting the Profile feature
- At least one email entry in the identity store for each user that could be invited.

#### Configuration
In your mxisd config file:
```yaml
synapseSql:
  enabled: false ## Do not use this line if Synapse is used as an Identity Store
  type: '<DB TYPE>'
  connection: '<DB CONNECTION URL>'
```

The `synapseSql` section is optional. It is used to retrieve display names which are not directly accessible in this mode.
For details about `type` and `connection`, see the [relevant documentation](../../stores/synapse.md).
If you do not configure it, some placeholders will not be available in the notification, like the Room name.

You can also change the default template of the notification using the `generic.matrixId` template option.  
See [the Template generator documentation](../../threepids/notification/template-generator.md) for more info.

#### Test
Invite a user which is part of your domain while an appropriate Identity store is used.

### Auto-reject of expired 3PID invites
*TBC*
