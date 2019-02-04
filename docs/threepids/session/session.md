# 3PID Sessions
- [Overview](#overview)
- [Restrictions](#restrictions)
  - [Bindings](#bindings)
  - [Federation](#federation)
- [Notifications](#notifications)
  - [Email](#email)
  - [Phone numbers](#msisdn-(phone-numbers))
- [Usage](#usage)
  - [Configuration](#configuration)
  - [Web views](#web-views)
  - [Scenarios](#scenarios)
    - [Sessions disabled](#sessions-disabled)

## Overview
When adding an email, a phone number or any other kind of 3PID (Third-Party Identifier) in a Matrix client,
the identity server is contacted to validate the 3PID.

To validate the 3PID, the identity server creates a session associated with a secret token. That token is sent via a message
to the 3PID (e.g. an email) with a the necessary info so the user can submit them to the Identity Server, confirm ownership
of the 3PID.

Once this 3PID is validated, the Homeserver will request that the Identity Server links the provided user Matrix ID with
the 3PID session and finally add the 3PID to its own data store.

This serves two purposes:
- Add the 3PID as an administrative/login info for the Homeserver directly
- Links, called *Bind*, the 3PID so it can be queried from Homeservers and clients when inviting someone in a room
by a 3PID, allowing it to be resolved to a Matrix ID.

## Restrictions
### Bindings
mxisd does not store bindings directly. While a user can see its email, phone number or any other 3PID in its
settings/profile, it does **NOT** mean it is published/saved anywhere or can be used to invite/search the user.

Identity stores are the ones holding such data, irrelevant if a user added a 3PID to their profile. When queried for
bindings, mxisd will query Identity stores which are responsible to store this kind of information.

Therefore, by default, any 3PID added to a user profile which is NOT within a configured and enabled Identity backend
will simply not be usable for search or invites, **even on the same Homeserver!**  

To have such 3PID bindings available for search and invite queries on synapse, use its dedicated
[Identity store](../../stores/synapse.md).

### Federation
In a federated set up, identity servers must cooperate to find the Matrix ID associated with a 3PID.

Federation is based on the principle that each server is responsible for its own (dns) domain.
Therefore only those 3PID can be federated that can be distinguished by their
domain such as email addresses.

Example: a user from Homeserver `example.org` adds an email `john@example.com`.  
Federated identity servers would try to find the identity server at `example.com` and ask it for the Matrix ID of associated with `john@example.com`.

Nevertheless, Matrix users might add 3PIDs that are not associated to a domain, for example telephone numbers.
Or they might even add 3PIDs associated to a different domain (such as an email address hosted by Gmail).
Such 3PIDs cannot be resolved in a federated way and will not be found from other servers.

Example: a user from Homeserver `example.org` adds an email `john@gmail.com`.  
If a federated lookup was performed, Identity servers would try to find the 3PID bind at the `gmail.com` server, and
not `example.org`.

As mxisd is built for self-hosted use cases, mainly for orgs/corps, this is usually not a problem for emails.  
Sadly, there is currently no mechanism to make this work for phone numbers. 

## Notifications
3PIDs are validated by sending a pre-formatted message containing a token to that 3PID address, which must be given to the
Identity server that received the request. This is usually done by means of a URL to visit for email or a short number
received by SMS for phone numbers.

mxisd use two components for this:
- Generator which produces the message to be sent with the necessary information the user needs to validate their session.
- Connector which actually send the notification (e.g. SMTP for email).

Built-in generators and connectors for supported 3PID types:

### Email
Generators:
- [Template](../notification/template-generator.md)

Connectors:
- [SMTP](../medium/email/smtp-connector.md)

#### MSISDN (Phone numbers)
Generators:
- [Template](../notification/template-generator.md)

Connectors:
 - [Twilio](../medium/msisdn/twilio-connector.md) with SMS

## Usage
### Configuration
The following example of configuration shows which items are relevant for 3PID sessions.

**IMPORTANT:** Most configuration items shown have default values and should not be included in your own configuration
file unless you want to specifically overwrite them.
```yaml
# CONFIGURATION EXAMPLE
# DO NOT COPY/PASTE AS-IS IN YOUR CONFIGURATION

session:
  policy:
    validation:
      enabled: true
    unbind:
      fraudulent:
        sendWarning: true

# DO NOT COPY/PASTE AS-IS IN YOUR CONFIGURATION
# CONFIGURATION EXAMPLE
```

`session.policy.validation` is the core configuration to control what users configured to use your Identity server
are allowed to do in terms of 3PID sessions. The policy has a global on/off switch for 3PID sessions using `.enabled`  

---

`unbind.fraudulent` controls warning notifications if an illegal/fraudulent 3PID removal is attempted on the Identity server.  
This is directly related to synapse disregard for privacy and new GDPR laws in Europe in an attempt to inform users about
potential privacy leaks.

For more information, see the corresponding [synapse issue](https://github.com/matrix-org/synapse/issues/4540).

### Web views
Once a user click on a validation link, it is taken to the Identity Server validation page where the token is submitted.  
If the session or token is invalid, an error page is displayed.  
Workflow pages are also available for the remote 3PID session process.

See [the dedicated document](session-views.md)
on how to configure/customize/brand those pages to your liking.

### Scenarios
#### Sessions disabled
This configuration would disable 3PID sessions altogether, preventing users from validating emails and/or phone numbers
and any subsequent actions that requires them, like adding them to their profiles.
  
This would be used if mxisd is also performing authentication for the Homeserver, typically with synapse and the
[REST password provider](https://github.com/kamax-matrix/matrix-synapse-rest-auth), where 3PID mappings would be
auto-populated.

Use the following values to enable this mode:
```yaml
session:
  policy:
    validation:
      enabled: false
```
