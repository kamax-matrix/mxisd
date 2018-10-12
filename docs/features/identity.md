# Identity
**WARNING**: This document is incomplete and can be missleading.

Implementation of the [Unofficial Matrix Identity Service API](https://kamax.io/matrix/api/identity_service/unstable.html).

## Lookups
If you would like to use the central matrix.org Identity server to ensure maximum discovery at the cost of potentially
leaking all your contacts information, add the following to your configuration:
```yaml
forward.servers:
  - 'matrix-org'
```
**NOTE:** You should carefully consider enabling this option, which is discouraged.  
For more info, see the [relevant issue](https://github.com/kamax-matrix/mxisd/issues/76).

## Room Invitations
Resolution can be customized using the following configuration:

`invite.resolution.recursive`  
- Default value: `true`  
- Description: Control if the pending invite resolution should be done recursively or not.  
  **DANGER ZONE:** This setting has the potential to create "an isolated island", which can have unexpected side effects
  and break invites in rooms. This will most likely not have the effect you think it does. Only change the value if you
  understand the consequences.

`invite.resolution.timer`  
- Default value: `1`  
- Description: How often, in minutes, mxisd should try to resolve pending invites.

## 3PID addition to user profile
See the [3PID session documents](../threepids/session)
