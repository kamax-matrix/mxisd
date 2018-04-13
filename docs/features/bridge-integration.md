# Bridge Integration
To help natural bridge integration into the regular usage of a Matrix client, mxisd provides a way for bridge to reply
to 3PID queries if no mapping was found, allowing seamless bridging to a network.

This is performed by implementing a specific endpoint on the bridge to map a 3PID lookup to a virtual user.

**NOTE**: This document is incomplete and might be misleading. In doubt, come in our Matrix room.  
You can also look at our [Email Bridge README](https://github.com/kamax-io/matrix-appservice-email#mxisd) for an example
of working configuration.

## Configuration
```yaml
lookup.recursive.bridge.enabled: <boolean>
lookup.recursive.bridge.recursiveOnly: <boolean>
lookup.recursive.bridge.server: <URL to the bridge endpoint for all 3PID medium>
lookup.recursive.bridge.mappings:
  <3PID MEDIUM HERE>: <URL to dedicated bridge for that medium>

```

## Integration
Implement a simplified version of the [Identity service single lookup endpoint](https://kamax.io/matrix/api/identity_service/unstable.html#get-matrix-identity-api-v1-lookup)
with only the following parameters needed:
- `address`
- `medium`
- `mxid`

Or an empty object if no resolution exists or desired.
