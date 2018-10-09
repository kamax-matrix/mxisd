# REST Identity store
The REST backend allows you to query identity data in existing webapps, like:
- Forums (phpBB, Discourse, etc.)
- Custom Identity stores (Keycloak, ...)
- CRMs (Wordpress, ...)
- self-hosted clouds (Nextcloud, ownCloud, ...)

To integrate this backend with your webapp, you will need to implement three specific REST endpoints detailed below.

## Features
|      Name      | Supported? |
|----------------|------------|
| Authentication | Yes        |
| Directory      | Yes        |
| Identity       | Yes        |
| Profile        | No         |

## Configuration
| Key                              | Default                                        | Description                                          |
|----------------------------------|------------------------------------------------|------------------------------------------------------|
| `rest.enabled`                   | `false`                                        | Globally enable/disable the REST backend             |
| `rest.host`                      | *None*                                        | Default base URL to use for the different endpoints. |
| `rest.endpoints.auth`            | `/_mxisd/backend/api/v1/auth/login`            | Validate credentials and get user profile            |
| `rest.endpoints.directory`       | `/_mxisd/backend/api/v1/directory/user/search` | Search for users by arbitrary input                  |
| `rest.endpoints.identity.single` | `/_mxisd/backend/api/v1/identity/single`       | Endpoint to query a single 3PID                      |
| `rest.endpoints.identity.bulk`   | `/_mxisd/backend/api/v1/identity/bulk`         | Endpoint to query a list of 3PID                     |

Endpoint values can handle two formats:
- URL Path starting with `/` that gets happened to the `rest.host`
- Full URL, if you want each endpoint to go to a specific server/protocol/port

`rest.host` is mandatory if at least one endpoint is not a full URL.

## Endpoints
### Authentication
HTTP method: `POST`  
Content-type: JSON UTF-8
  
#### Request Body
```json
{
  "auth": {
    "mxid": "@john.doe:example.org",
    "localpart": "john.doe",
    "domain": "example.org",
    "password": "passwordOfTheUser"
  }
}
```

#### Response Body
If the authentication fails:
```json
{
  "auth": {
    "success": false
  }
}
```

If the authentication succeed:
- `auth.id` supported values: `localpart`, `mxid`
- `auth.profile` and any sub-member are all optional
```json
{
  "auth": {
    "success": true,
    "id": {
      "type": "localpart",
      "value": "john"
    },
    "profile": {
      "display_name": "John Doe",
      "three_pids": [
        {
          "medium": "email",
          "address": "john.doe@example.org"
        },
        {
          "medium": "msisdn",
          "address": "123456789"
        }
      ]
    }
  }
}
```

### Directory
HTTP method: `POST`
Content-type: JSON UTF-8

#### Request Body
```json
{
  "by": "<search type>",
  "search_term": "doe"
}
```
`by` can be:
- `name`
- `threepid`

#### Response Body:
If users found:
```json
{
  "limited": false,
  "results": [
    {
      "avatar_url": "http://domain.tld/path/to/avatar.png",
      "display_name": "John Doe",
      "user_id": "UserIdLocalpart"
    },
    {
      ...
    }
  ]
}
```

If no user found:
```json
{
  "limited": false,
  "results": []
}
```

### Identity
#### Single 3PID lookup
HTTP method: `POST`  
Content-type: JSON UTF-8  
  
#### Request Body
```json
{
  "lookup": {
    "medium": "email",
    "address": "john.doe@example.org"
  }
}
```

#### Response Body
If a match was found:
- `lookup.id.type` supported values: `localpart`, `mxid`
```json
{
  "lookup": {
    "medium": "email",
    "address": "john.doe@example.org",
    "id": {
      "type": "mxid",
      "value": "@john:example.org"
    }
  }
}
```

If no match was found:
```json
{}
```

#### Bulk 3PID lookup
HTTP method: `POST`  
Content-type: JSON UTF-8  
  
#### Request Body
```json
{
  "lookup": [
    {
      "medium": "email",
      "address": "john.doe@example.org"
    },
    {
      "medium": "msisdn",
      "address": "123456789"
    }
  ]
}
```

#### Response Body
For all entries where a match was found:
- `lookup[].id.type` supported values: `localpart`, `mxid`
```json
{
  "lookup": [
    {
      "medium": "email",
      "address": "john.doe@example.org",
      "id": {
        "type": "localpart",
        "value": "john"
      }
    },
    {
      "medium": "msisdn",
      "address": "123456789",
      "id": {
        "type": "mxid",
        "value": "@jane:example.org"
      }
    }
  ]
}
```

If no match was found:
```json
{
  "lookup": []
}
```
