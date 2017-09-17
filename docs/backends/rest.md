# REST backend
The REST backend allows you to query arbitrary REST JSON endpoints as backends for the following flows:
- Identity lookup
- Authentication

## Configuration
| Key                            | Default                               | Description                                          |
---------------------------------|---------------------------------------|------------------------------------------------------|
| rest.enabled                   | false                                 | Globally enable/disable the REST backend             |
| rest.host                      | *empty*                               | Default base URL to use for the different endpoints. |
| rest.endpoints.auth            | /_mxisd/identity/api/v1/auth          | Endpoint to validate credentials                     |
| rest.endpoints.identity.single | /_mxisd/identity/api/v1/lookup/single | Endpoint to lookup a single 3PID                     |
| rest.endpoints.identity.bulk   | /_mxisd/identity/api/v1/lookup/bulk   | Endpoint to lookup a list of 3PID                    |

Endpoint values can handle two formats:
- URL Path starting with `/` that gets happened to the `rest.host`
- Full URL, if you want each endpoint to go to a specific server/protocol/port

`rest.host` is only mandatory if at least one endpoint is not a full URL.

## Endpoints
### Authenticate
Configured with `rest.endpoints.auth`

HTTP method: `POST`  
Encoding: JSON UTF-8
  
#### Request Body
```
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
```
{
  "auth": {
    "success": false
  }
}
```

If the authentication succeed:
- `auth.id` supported values: `localpart`, `mxid`
- `auth.profile` and any sub-member are all optional
```
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

### Lookup
#### Single
Configured with `rest.endpoints.identity.single`

HTTP method: `POST`  
Encoding: JSON UTF-8  
  
#### Request Body
```
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
```
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
```
{}
```

#### Bulk
Configured with `rest.endpoints.identity.bulk`

HTTP method: `POST`  
Encoding: JSON UTF-8  
  
#### Request Body
```
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
```
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
```
{
  "lookup": []
}
```