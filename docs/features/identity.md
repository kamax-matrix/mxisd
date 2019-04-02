# Identity
Implementation of the [Identity Service API r0.1.0](https://matrix.org/docs/spec/identity_service/r0.1.0.html).

- [Lookups](#lookups)
- [Invitations](#invitations)
  - [Expiration](#expiration)
  - [Policies](#policies)
  - [Resolution](#resolution)
- [3PIDs Management](#3pids-management)

## Lookups
If you would like to use the central matrix.org Identity server to ensure maximum discovery at the cost of potentially
leaking all your contacts information, add the following to your configuration:
```yaml
forward:
  servers:
    - 'matrix-org'
```
**NOTE:** You should carefully consider enabling this option, which is discouraged.  
For more info, see the [relevant issue](https://github.com/kamax-matrix/mxisd/issues/76).

## Invitations
### Expiration
#### Overview
Matrix does not provide a mean to remove/cancel pending 3PID invitations with the APIs. The current reference
implementations also do not provide any mean to do so. This leads to 3PID invites forever stuck in rooms.

To provide this functionality, mxisd uses a workaround: resolve the invite to a dedicated User ID, which can be
controlled by mxisd or a bot/service that will then reject the invite.

If this dedicated User ID is to be controlled by mxisd, the [Application Service](experimental/application-service.md)
feature must be configured and integrated with your Homeserver.

#### Configuration
```yaml
invite:
  expiration:
    enabled: true/false
    after: 5
    resolveTo: '@john.doe:example.org'
```
`enabled`
- Purpose: Enable or disable the invite expiration feature.
- Default: `true`

`after` 
- Purpose: Amount of minutes before an invitation expires.
- Default: `10080` (7 days)

`resolveTo`
- Purpose: Matrix User ID to resolve the expired invitations to.
- Default: Computed from `appsvc.user.inviteExpired` and `matrix.domain`

### Policies
#### Integration
##### Reverse Proxy
###### nginx
```nginx
location ~* ^/_matrix/client/r0/rooms/([^/]+)/invite$ {
    proxy_pass		    http://127.0.0.1:8090;
    proxy_set_header	Host $host;
    proxy_set_header	X-Forwarded-For $remote_addr;
}
```

##### Configuration
```yaml
invite:
  policy:
    ifSender:
      hasRole:
        - '<THIS_ROLE>'
        - '<OR_THIS_ROLE>'
```

### Resolution
Resolution of 3PID invitations can be customized using the following configuration:

`invite.resolution.recursive`  
- Default value: `true`  
- Description: Control if the pending invite resolution should be done recursively or not.  
  **DANGER ZONE:** This setting has the potential to create "an isolated island", which can have unexpected side effects
  and break invites in rooms. This will most likely not have the effect you think it does. Only change the value if you
  understand the consequences.

`invite.resolution.timer`  
- Default value: `1`  
- Description: How often, in minutes, mxisd should try to resolve pending invites.

## 3PIDs Management
See the [3PID session documents](../threepids/session)
