# Notifications: Generate from templates
To create notification content, you can use the `template` generator if supported for the 3PID medium which will read
content from configured files.

Placeholders can be integrated into the templates to dynamically populate such content with relevant information like
the 3PID that was requested, the domain of your Identity server, etc.

Templates can be configured for each event that would send a notification to the end user. Events share a set of common
placeholders and also have their own individual set of placeholders.

## Configuration
To configure paths to the various templates:
```
threepid:
  medium:
    <YOUR 3PID MEDIUM HERE>:
      generators:
        template:
          invite: '/path/to/invite-template.eml'
          session:
            validation:
              local: '/path/to/validate-local-template.eml'
              remote: 'path/to/validate-remote-template.eml'
```
The `template` generator is usually the default, so no further configuration is needed.

##  Global placeholders
| Placeholder           | Purpose                                                                      |
|-----------------------|------------------------------------------------------------------------------|
| `%DOMAIN%`            | Identity server authoritative domain, as configured in `matrix.domain`       |
| `%DOMAIN_PRETTY%`     | Same as `%DOMAIN%` with the first letter upper case and all other lower case |
| `%FROM_EMAIL%`        | Email address configured in `threepid.medium.<3PID medium>.identity.from`    |
| `%FROM_NAME%`         | Name configured in `threepid.medium.<3PID medium>.identity.name`             |
| `%RECIPIENT_MEDIUM%`  | The 3PID medium, like `email` or `msisdn`                                    |
| `%RECIPIENT_ADDRESS%` | The address to which the notification is sent                                |

## Events
### Room invitation
This template is used when someone is invited into a room using an email address which has no known bind to a Matrix ID.
#### Placeholders
| Placeholder           | Purpose                                                                                  |
|-----------------------|------------------------------------------------------------------------------------------|
| `%SENDER_ID%`         | Matrix ID of the user who made the invite                                                |
| `%SENDER_NAME%`       | Display name of the user who made the invite, if not available/set, empty                |
| `%SENDER_NAME_OR_ID%` | Display name of the user who made the invite. If not available/set, its Matrix ID        |
| `%INVITE_MEDIUM%`     | The 3PID medium for the invite.                                                          |
| `%INVITE_ADDRESS%`    | The 3PID address for the invite.                                                         |
| `%ROOM_ID%`           | The Matrix ID of the Room in which the invite took place                                 |
| `%ROOM_NAME%`         | The Name of the room in which the invite took place. If not available/set, empty         |
| `%ROOM_NAME_OR_ID%`   | The Name of the room in which the invite took place. If not available/set, its Matrix ID |

### Local validation of 3PID Session
This template is used when to user which added their 3PID address to their profile/settings and the session policy
allows at least local sessions.  

#### Placeholders
| Placeholder          | Purpose                                                                              |
|----------------------|--------------------------------------------------------------------------------------|
| `%VALIDATION_LINK%`  | URL, including token, to validate the 3PID session.                                  |
| `%VALIDATION_TOKEN%` | The token needed to validate the local session, in case the user cannot use the link |

### Remote validation of 3PID Session
This template is used when to user which added their 3PID address to their profile/settings and the session policy only
allows remote sessions.

**NOTE:** 3PID session always require local validation of a token, even if a remote session is enforced.  
One cannot bind a Matrix ID to the session until both local and remote sessions have been validated.

#### Placeholders
| Placeholder          | Purpose                                                |
|----------------------|--------------------------------------------------------|
| `%VALIDATION_TOKEN%` | The token needed to validate the session               |
| `%NEXT_URL%`         | URL to continue with remote validation of the session. |
