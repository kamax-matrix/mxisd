# Exec Identity Store
This Identity Store lets you run arbitrary commands to handle the various requests in each support feature.

This is the most versatile Identity store of mxisd, allowing you to connect any kind of logic in any language/scripting.

## Features
|      Name      |   Supported?  |
|----------------|---------------|
| Authentication | Yes           |
| Directory      | *In Progress* |
| Identity       | *In Progress* |
| Profile        | *In Progress* |

## Overview
Each request can be mapping to a fully customizable command configuration.  
The various parameters can be provided via any combination of:
- Standard Input
- Command line arguments
- Environment variables

Each of those supports a set of customizable token which will be replaced prior to running the command, allowing to
provide the input values in any number of ways.

Success and data will be provided via [Exit status](https://en.wikipedia.org/wiki/Exit_status) and Standard Output, both
supporting a set of options. 

## Configuration
```yaml
exec.enabled: <boolean>
```
Enable/disable the Identity store at a global/default level. Each feature can still be enabled/disabled specifically.

*TBC*

## Use-case examples
```yaml
exec.enabled: true

exec.auth.command: '/path/to/auth/executable'
exec.auth.args: ['-u', '{localpart}']
exec.auth.env:
  PASSWORD: '{password}'
  MATRIX_DOMAIN: '{domain}'
  MATRIX_USER_ID: '{mxid}'
```
This will run `/path/to/auth/executable` with:
- The extracted Matrix User ID `localpart` provided as the second command line argument, the first one being `-u` 
- The password, the extract Matrix `domain` and the full User ID as arbitrary environment variables, respectively `PASSWORD`, `MATRIX_DOMAIN` and `MATRIX_USER_ID`

```yaml
## Few more available config items
#
# exec.token.domain: '{matrixDomain}' # This sets the default replacement token for the Matrix Domain of the User ID, across all features.
# exec.auth.token.domain: '{matrixDomainForAuth}' # We can also set another token specific to a feature.
# exec.auth.input: 'json' # This is not supported yet.
# exec.auth.exit.success: [0] # Exit status that will consider the request successful. This is already the default.
# exec.auth.exit.failure: [1,2,3] # Exist status that will consider the request failed. Anything else than success or failure statuses will throw an exception.
# exec.auth.output: 'json' # Required if stdout should be read on success. This uses the same output as the REST Identity store for Auth.
```
*TBC*
