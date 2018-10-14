# Integration as an Application Service
**WARNING:** These features are currently highly experimental. They can be removed or modified without notice.  
All the features requires a Homeserver capable of connecting Application Services.

## Email notification for Room invites by Matrix ID
This feature allows for users found in Identity stores to be instantly notified about Room Invites, regardless if their
account was already provisioned on the Homeserver.

### Requirements
- [Identity store(s)](../../stores/README.md) supporting the Profile feature
- At least one email entry in the identity store for each user that could be invited.

### Configuration
In your mxisd config file:
```yaml
matrix:
  listener:
    url:  '<URL TO THE CS API OF THE HOMESERVER>'
    localpart: 'appservice-mxisd'
    token:
      hs: 'HS_TOKEN_CHANGE_ME'

synapseSql:
  enabled: false ## Do not use this line if Synapse is used as an Identity Store
  type: '<DB TYPE>'
  connection: '<DB CONNECTION URL>'
```

The `synapseSql` section is used to retrieve display names which are not directly accessible in this mode.
For details about `type` and `connection`, see the [relevant documentation](../../stores/synapse.md).
If you do not configure it, some placeholders will not be available in the notification, like the Room name.

You can also change the default template of the notification using the `generic.matrixId` template option.  
See [the Template generator documentation](../../threepids/notification/template-generator.md) for more info.

### Homeserver integration
#### Synapse
Create a new appservice registration file. Futher config will assume it is in `/etc/matrix-synapse/appservice-mxisd.yaml`
```yaml
id: "appservice-mxisd"
url: "http://127.0.0.1:8090"
as_token: "AS_TOKEN_CHANGE_ME"
hs_token: "HS_TOKEN_CHANGE_ME"
sender_localpart: "appservice-mxisd"
namespaces:
  users:
    - regex: "@*"
      exclusive: false
  aliases: []
  rooms: []
```
`id`: An arbitrary unique string to identify the AS.  
`url`: mxisd to reach mxisd. This ideally should be HTTP and not going through any reverse proxy.  
`as_token`: Arbitrary value used by mxisd when talking to the HS. Not currently used.  
`hs_token`: Arbitrary value used by synapse when talking to mxisd. Must match `token.hs` in mxisd config.
`sender_localpart`: Username for the mxisd itself on the HS. Default configuration should be kept.  
`namespaces`: To be kept as is.  

Edit your `homeserver.yaml` and add a new entry to the appservice config file, which should look something like this:
```yaml
app_service_config_files:
  - '/etc/matrix-synapse/appservice-mxisd.yaml'
  - ...
```

Restart synapse when done to register mxisd.

#### Others
See your Homeserver documentation on how to integrate.

### Test
Invite a user which is part of your domain while an appropriate Identity store is used.
