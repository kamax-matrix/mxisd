# Registration
- [Overview](#overview)
- [Integration](#integration)
  - [Reverse Proxy](#reverse-proxy)
    - [nginx](#nginx)
    - [Apache](#apache)
  - [Homeserver](#homeserver)
    - [synapse](#synapse)
- [Configuration](#configuration)
  - [Example](#example)
- [Usage](#usage)

## Overview
**NOTE**: This feature is beta: it is considered stable enough for production but is incomplete and may contain bugs.

Registration is an enhanced feature of mxisd to control registrations involving 3PIDs on a Homeserver based on policies:
- Match pending 3PID invites on the server
- Match 3PID pattern, like a specific set of domains for emails
- In futher releases, use 3PIDs found in Identity stores

It aims to help open or invite-only registration servers control what is possible to do and ensure only approved people
can register on a given server in a implementation-agnostic manner.

**IMPORTANT:** This feature does not control registration in general. It only acts on endpoints related to 3PIDs during
the registration process.  
As such, it relies on the homeserver to require 3PIDs with the registration flows.

This feature is not part of the Matrix Identity Server spec.

## Integration
mxisd needs to be integrated at several levels for this feature to work:
- Reverse proxy: intercept the 3PID register endpoints and act on them
- Homeserver: require 3PID to be part of the registration data

Later version(s) of this feature may directly control registration itself to create a coherent experience
### Reverse Proxy
#### nginx
```nginx
location ^/_matrix/client/r0/register/[^/]/?$ {
	proxy_pass		http://127.0.0.1:8090;
	proxy_set_header	Host $host;
	proxy_set_header	X-Forwarded-For $remote_addr;
}
```

#### apache
> TBC

### Homeserver
#### Synapse
```yaml
enable_registration: true
registrations_require_3pid:
  - email
```

## Configuration
See the [Configuration](../configuration.md) introduction doc on how to read the configuration keys.  
An example of working configuration is avaiable at the end of this section.
### Enable/Disable
`register.allowed`, taking a boolean, can be used to enable/disable registration if the attempt is not 3PID-based.  
`false` is the default value to prevent open registration, as you must allow it on the homeserver side.

### For invites
`register.invite`, taking a boolean, controls if registration can be made using a 3PID which matches a pending 3PID invite.  
`true` is the default value.

### 3PID-specific
At this time, only `email` is supported with 3PID specific configuration with this feature.

#### Email
**Base key**: `register.threepid.email`

##### Domain whitelist/blacklist
If you would like to control which domains are allowed to be used when registrating with an email, the following sub-keys
are available:
- `domain.whitelist`
- `domain.blacklist`

The value format is an hybrid between glob patterns and postfix configuration files with the following syntax:
- `*<domain>` will match the domain and any sub-domain(s)
- `.<domain>` will only match sub-domain(s)
- `<domain>` will only match the exact domain

The following table illustrates pattern and maching status against example values:

| Config value   | Matches `example.org` | Matches `sub.example.org` |
|--------------- |-----------------------|---------------------------|
| `*example.org` | Yes                   | Yes                       |
| `.example.org` | No                    | Yes                       |
| `example.org`  | Yes                   | No                        |

### Example
For the following example configuration:
```yaml
register:
  policy:
    threepid:
      email:
        domain:
          whitelist:
            - '*example.org'
            - '.example.net'
            - 'example.com'
```
- Users can register using 3PIDs of pending invites, being allowed by default.
- Users can register using an email from `example.org` and any sub-domain, only sub-domains of `example.net` and `example.com` but not its sub-domains.
- Otherwise, user registration will be denied.

## Usage
Nothing special is needed. Register using a regular Matrix client.
