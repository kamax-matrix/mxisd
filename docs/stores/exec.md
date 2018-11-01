# Exec Identity Store
- [Features](#features)
- [Overview](#overview)
- [Configuration](#configuration)
  - [Global](#global)
    - [Tokens](#tokens)
  - [Executable](#executable)
    - [Input](#input)
    - [Output](#output)
  - [Examples](#examples)
  - [Per-Feature](#per-feature)
- [Authentication](#authentication)
  - [Tokens](#tokens-1)
  - [Input](#input-1)
  - [Output](#output-1)
- [Directory](#directory)
  - [Tokens](#tokens-2)
  - [Input](#input-2)
  - [Output](#output-2)
- [Identity](#identity)
  - [Single Lookup](#single-lookup)
    - [Tokens](#tokens-3)
    - [Input](#input-3)
    - [Output](#output-3)
  - [Bulk Lookup](#bulk-lookup)
    - [Tokens](#tokens-4)
    - [Input](#input-4)
    - [Output](#output-4)
- [Profile](#profile)
  - [Tokens](#tokens-5)
  - [Input](#input-5)
  - [Output](#output-5)
  
---

## Features
|                       Name                      | Supported |
|-------------------------------------------------|-----------|
| [Authentication](../features/authentication.md) | Yes       |
| [Directory](../features/directory.md)           | Yes       |
| [Identity](../features/identity.md)             | Yes       |
| [Profile](#profile)                             | Yes       |

This Identity Store lets you run arbitrary commands to handle the various requests in each support feature.  
It is the most versatile Identity store of mxisd, allowing you to connect any kind of logic with any executable/script.

## Overview
Each request can be mapping to a fully customizable command configuration.  
The various parameters can be provided via any combination of:
- [Standard Input](https://en.wikipedia.org/wiki/Standard_streams#Standard_input_(stdin))
- [Command-line arguments](https://en.wikipedia.org/wiki/Command-line_interface#Arguments)
- [Environment variables](https://en.wikipedia.org/wiki/Environment_variable)

Each of those supports a set of customizable token which will be replaced prior to running the command, allowing to
provide the input values in any number of ways.

Success and data will be provided via any combination of:
- [Exit status](https://en.wikipedia.org/wiki/Exit_status)
- [Standard Output](https://en.wikipedia.org/wiki/Standard_streams#Standard_output_(stdout))

Each of those supports a set of configuration item to decide how to process the value and/or in which format.

All values, inputs and outputs are UTF-8 encoded.

## Configuration
Each feature comes with a set of possible lookup/action which is mapped to a generic configuration item block.  
We will use the term `Executable` for each lookup/action and `Processor` for each configuration block.

### Global
```yaml
exec.enabled: <boolean>
```
Enable/disable the Identity store at a global/default level. Each feature can still be individually enabled/disabled.

#### Tokens
The following options allow to globally set tokens for value replacement across all features and processors config.  
Not all features use all tokens, and each feature might also have its own specific tokens. See each feature documentation.
  
They can be set within the following scope:

```yaml
exec.token.<token>: '<value>'
```

---

The following tokens and default values are available:
```yaml
localpart: '{localpart}'
```
Localpart of Matrix User IDs

```yaml
domain: '{domain}'
```
Domain of Matrix User IDs

```yaml
mxid: '{mxid}'
```
Full representation of Matrix User IDs

```yaml
medium: '{medium}'
```
Medium of 3PIDs

```yaml
address: '{address}'
```
Address of 3PIDs

```yaml
type: '{type}'
```
Type of query

```yaml
query: '{query}'
```
Query value

### Executable
*Executable*s have the following options:
```yaml
command: '/path/to/executableOrScript'

```
Set the executable (relative or absolute) path to be executed. If no command is given, the action will return a "neutral"
result if possible or be skipped altogether.

---

Command line arguments can be given via a list via both YAML formats:
```yaml
args:
 - '-t'
 - '{token}'
 - '-v'
 - 'value'
```
or
```yaml
args: ['-t', '{token}', '-v', 'value]
```
Each argument will be processed for token replacement.

---

Environment variables can be given as key/value pairs:
```yaml
env:
  ENV_VAR_1: 'value'
  ENV_VAR_2: '{token}'
``` 
Each variable value will be processed for token replacement.

#### Input
Standard input can be configured in the namespaces `input` with:
- `type`: The format to use
- `template`: The full or partial template with tokens to be used when generating the input

Not all features and *Executable*s allow for a template to be provided.  
Templates for listed-based input are not supported at this time.  
Default templates may be provided per *Executable*.

The following types are available:
- `json`: Use JSON format, shared with the [REST Identity Store](rest.md)
- `plain`: Use a custom multi-lines, optionally tab-separated input

#### Output
Standard output can be configured in the namespaces `output` with:
- `type`: The format to use
- `template`: The full or partial template with tokens to be used when processing the output

Not all features and *Executable*s allow for a template to be provided.  
Templates for listed-based output are not supported at this time.  
Default templates may be provided per *Executable*.

The following types are available:
- `json`: Use JSON format, shared with the [REST Identity Store](rest.md)
- `plain`: Use a custom multi-lines, optionally tab-separated output

### Examples
#### Basic
```yaml
exec.auth.enabled: true
exec.auth.command: '/opt/mxisd-exec/auth.sh'
exec.auth.args: ['{localpart}']
exec.auth.input.type: 'plain'
exec.auth.input.template: '{password}'
exec.auth.env:
  DOMAIN: '{domain}'
```
With Authentication enabled, run `/opt/mxisd-exec/auth.sh` when validating credentials, providing:
- A single command-line argument to provide the `localoart` as username 
- A plain text string with the password token for standard input, which will be replaced by the password to check
- A single environment variable `DOMAIN` containing Matrix ID domain, if given

The command will use the default values for:
- Success exit status of `0`
- Failure exit status of `1`
- Any other exit status considered as error
- The standard output processing as not processed

#### Advanced
Given the fictional `placeholder` feature:
```yaml
exec.enabled: true
exec.token.mxid: '{matrixId}'

exec.placeholder.token.localpart: '{username}'
exec.placeholder.command: '/path/to/executable'
exec.placeholder.args:
  - '-u'
  - '{username}'
exec.placeholder.env:
  MATRIX_DOMAIN: '{domain}'
  MATRIX_USER_ID: '{matrixId}'
  
exec.placeholder.output.type: 'json'
exec.placeholder.exit.success: [0, 128]
exec.placeholder.exit.failure: [1, 129]
```
With:
- The Identity store enabled for all features
- A global specific token `{matrixId}` for Matrix User IDs, replacing the default `{mxid}`

Running `/path/to/executable` providing:
- A custom token for localpart, `{username}`, used as a 2nd command-line argument
- An extracted Matrix User ID `localpart` provided as the second command line argument, the first one being `-u` 
- A password, the extracted Matrix `domain` and the full User ID as arbitrary environment variables, respectively
  `PASSWORD`, `MATRIX_DOMAIN` and `MATRIX_USER_ID`

After execution:
- Process stdout as [JSON](https://en.wikipedia.org/wiki/JSON)
- Consider exit status `0` and `128` as success and try to process the stdout for data
- Consider exit status `1` and `129` as failure and try to process the stdout for error code and message

### Per Feature
See each dedicated [Feature](#features) section.

## Authentication
The Authentication feature can be enabled/disabled using:
```yaml
exec.auth.enabled: <true/false>
```

---

This feature provides a single *Executable* under the namespace:
```yaml
exec.auth:
  ...
```

### Tokens
The following tokens/default values are specific to this feature:
```yaml
password: '{password}'
```
The provided password

### Input
Supported input types and default templates:

#### JSON (`json`)
Same as the [REST Identity Store](rest.md);

#### Plain (`plain`)
Default template:
```
{localpart}
{domain}
{mxid}
{password}
```

### Output
Supported output types and default templates:

#### JSON (`json`)
Same as the [REST Identity Store](rest.md);

#### Plain (`plain`)
**NOTE:** This has limited support. Use the JSON type for full support.

Default template:
```
[success status, true or 1 are interpreted as success]
[display name of the user]
```

## Directory
The Directory feature can be enabled/disabled using:
```yaml
exec.directory.enabled: <true/false>
```

---

Two search types configuration namespace are available, using the same input/output formats and templates:

By name:
```yaml
exec.directory.search.byName:
  ...
```
By 3PID:
```yaml
exec.directory.search.byThreepid:
  ...
```

#### Tokens
No specific tokens are available.

#### Input
Supported input types and default templates:

##### JSON (`json`)
Same as the [REST Identity Store](rest.md);

##### Plain (`plain`)
Default template:
```
[type of search, following the REST Identity store format]
[query string]
```

#### Output
Supported output types and default templates:

##### JSON (`json`)
Same as the [REST Identity Store](rest.md);

##### Plain (`plain`)
**Not supported at this time.** Use the JSON type.

## Identity
The Identity feature can be enabled/disabled using:
```yaml
exec.identity.enabled: <true/false>
```

### Single lookup
Configuration namespace:
```yaml
exec.identity.lookup.single:
  ...
```

#### Tokens
No specific tokens are available.

#### Input
Supported input types and default templates:

##### JSON (`json`)
Same as the [REST Identity Store](rest.md);

##### Plain (`plain`)
Default template:
```
{medium}
{address}
```

#### Output
Supported output types and default templates:

##### JSON (`json`)
Same as the [REST Identity Store](rest.md);

##### Plain (`plain`)
Default template:
```
[User ID type, as documented in the REST Identity Store]
[User ID value]
```

The User ID type will default to `localpart` if:
- Only one line is returned
- The first line is empty

### Bulk lookup
Configuration namespace:
```yaml
exec.identity.lookup.bulk:
  ...
```

#### Tokens
No specific tokens are available.

#### Input
Supported input types and default templates:

##### JSON (`json`)
**NOTE:** Custom Templates are not supported.

Same as the [REST Identity Store](rest.md).

##### Plain (`plain`)
**Not supported at this time.** Use the JSON type.

#### Output
Supported output types and default templates:

##### JSON (`json`)
**NOTE:** Custom Templates are not supported.

Same as the [REST Identity Store](rest.md).

##### Plain (`plain`)
**Not supported at this time.** Use the JSON type.

## Profile
The Profile feature can be enabled/disabled using:
```yaml
exec.profile.enabled: <true/false>
```

---

The following *Executable*s namespace are available, share the same input/output formats and templates:

Get Display name:
```yaml
exec.profile.displayName:
  ...
```

Get 3PIDs:
```yaml
exec.profile.threePid:
  ...
```

Get Roles:
```yaml
exec.profile.role:
  ...
```


### Tokens
No specific tokens are available.

### Input
Supported input types and default templates:

#### JSON (`json`)
Same as the [REST Identity Store](rest.md);

#### Plain (`plain`)
Default template:
```
{localpart}
{domain}
{mxid}
```
### Output
Supported output types and default templates:

#### JSON (`json`)
Same as the [REST Identity Store](rest.md);

#### Plain (`plain`)
**Not supported at this time.** Use the JSON type.
