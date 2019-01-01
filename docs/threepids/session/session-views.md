# Web pages for the 3PID sessions
You can customize the various pages used during a 3PID validation using the options below.

## Configuration
Pseudo-configuration to illustrate the structure:
```yaml
# CONFIGURATION EXAMPLE
# DO NOT COPY/PASTE THIS IN YOUR CONFIGURATION
view:
  session:
    local:
      onTokenSubmit:
        success: '/path/to/session/local/tokenSubmitSuccess-page.html'
        failure: '/path/to/session/local/tokenSubmitFailure-page.html'
    localRemote:
      onTokenSubmit:
        success: '/path/to/session/localRemote/tokenSubmitSuccess-page.html'
        failure: '/path/to/session/local/tokenSubmitFailure-page.html'
    remote:
      onRequest:
        success: '/path/to/session/remote/requestSuccess-page.html'
        failure: '/path/to/session/remote/requestFailure-page.html'
      onCheck:
        success: '/path/to/session/remote/checkSuccess-page.html'
        failure: '/path/to/session/remote/checkFailure-page.html'
# CONFIGURATION EXAMPLE
# DO NOT COPY/PASTE THIS IN YOUR CONFIGURATION
```

3PID session are divided into three config sections:
- `local` for local-only 3PID sessions
- `localRemote` for local 3PID sessions that can also be turned into remote sessions, if the user so desires
- `remote` for remote-only 3PID sessions

Each section contains a sub-key per support event. Finally, a `success` and `failure` key is available depending on the
outcome of the request.

## Local
### onTokenSubmit
This is triggered when a user submit a validation token for a 3PID session. It is typically visited when clicking the
link in a validation email.

The template should typically inform the user that the validation was successful and to go back in their Matrix client
to finish the validation process.

#### Placeholders
No object/placeholder are currently available.

## Local & Remote
### onTokenSubmit
This is triggered when a user submit a validation token for a 3PID session. It is typically visited when clicking the
link in a validation email.

The template should typically inform the user that their 3PID address will not yet be publicly/globally usable. In case
they want to make it, they should start a Remote 3PID session with a given link or that they can go back to their Matrix
client if they do not wish to proceed any further.

#### Placeholders
##### Success
`<a href="${remoteSessionLink}">text</a>` can be used to display the link to start a Remote 3PID session.

##### Failure
No object/placeholder are currently available.

## Remote
### onRequest
This is triggered when a user starts a Remote 3PID session, usually from a link produced in the `local.onTokenSubmit`
view or in a remote-only 3PID notification.

The template should typically inform the user that the remote creation was successful, followed the instructions sent by
the remote Identity server and, once that is done, click a link to validate the session.

#### Placeholders
##### Success
`<a href="${checkLink}">text</a>` can be used to display the link to validate the Remote 3PID session.

##### Failure
No object/placeholder are currently available.

### onCheck
This is triggered when a user attempts to inform the Identity server that the Remote 3PID session has been validated
with the remote Identity server.

The template should typically inform the user that the validation was successful and to go back in their Matrix client
to finish the validation process.

#### Placeholders
No object/placeholder are currently available.
