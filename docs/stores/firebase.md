# Google Firebase Identity store
https://firebase.google.com/

## Features
|                       Name                      | Supported |
|-------------------------------------------------|-----------|
| [Authentication](../features/authentication.md) | Yes       |
| [Directory](../features/directory.md)           | No        |
| [Identity](../features/identity.md)             | Yes       |
| [Profile](../features/profile.md)               | No        |

## Requirements
This backend requires a suitable Matrix client capable of performing Firebase authentication and passing the following
information:
- Firebase User ID as Matrix username
- Firebase token as Matrix password

If your client is Riot, you will need a custom version.

## Configuration
```yaml
firebase:
  enabled: <boolean>
```
Enable/disable this identity store.

Example:
```yaml
firebase:
  enabled: <boolean>
```

---

```yaml
firebase:
  credentials: <string>
```
Path to the credentials file provided by Google Firebase to use with an external app.

Example:
```yaml
firebase:
  credentials: '/path/to/firebase/credentials.json'
```

---

```yaml
firebase:
  database: <string>
```
URL to your Firebase database.

Example:
```yaml
firebase:
  database: 'https://my-project.firebaseio.com/'
```
