# AD/Samba/LDAP backend
## Configuration
### Structure and default values
```
ldap:
  enabled: false
  filter: ''
  connection:
    host: ''
    tls: false
    port: 389
    bindDn: ''
    bindPassword: ''
    baseDn: ''
  attribute:
    uid:
      type: 'uid'
      value: 'userPrincipalName'
    name: 'displayName'
    threepid:
      email:
        - 'mailPrimaryAddress'
        - 'mail'
        - 'otherMailbox'
      msisdn:
        - 'telephoneNumber'
        - 'mobile'
        - 'homePhone'
        - 'otherTelephone'
        - 'otherMobile'
        - 'otherHomePhone'
  auth:
    filter: ''
  directory:
    attribute:
      other: []
    filter: ''
  identity:
    filter: ''
    medium:
      email: ''
      msisdn: ''
```
### General
| Item      | Description                                                                               |
|-----------|-------------------------------------------------------------------------------------------|
| `enabled` | Globaly enable/disable the LDAP backend                                                   |
| `filter`  | Global filter to apply on all LDAP queries. Can be overwritten in each applicable section |

### Connection
| Item           | Description                                          |
|----------------|------------------------------------------------------|
| `host`         | Host to connect to                                   |
| `port`         | Port to use                                          |
| `tls`          | boolean to use TLS or not (STARTLS is not supported) |
| `bindDn`       | Bind DN for authentication                           |
| `bindPassword` | Bind password                                        |
| `baseDn`       | Base DN for queries                                  |

### Attributes
| Item        | Description                                                                                                            |
|-------------|------------------------------------------------------------------------------------------------------------------------|
| `uid.type`  | Indicate how to process the User ID (UID) attribute:                                                                   |
|             |   - `uid` will consider the value as the [Localpart](https://matrix.org/docs/spec/intro.html#user-identifiers)         |
|             |   - `mxid` will consider the value as a complete [Matrix ID](https://matrix.org/docs/spec/intro.html#user-identifiers) |
| `uid.value` | Attribute name refering to the User ID. This is typically `userPrincipalName` on AD/Samba setups and `uid` in LDAP     |
| `name`      | Attribute name that contains the [Display Name](https://matrix.org/docs/spec/intro.html#profiles) of the user          |
| `threepid`  | Namespace where each key is a 3PID type and contains a list of attributes                                              |

### Authentication
| Item     | Description                                                                                      |
|----------|--------------------------------------------------------------------------------------------------|
| `filter` | Specific user filter applied during authentication. Global filter is used if empty/blank/not set |

### Directory
| Item              | Description                                                         |
|-------------------|---------------------------------------------------------------------|
| `attribute.other` | Additional attributes to be used when performing directory searches |
| `filter`          | Specific user filter applied during directory search.               |
|                   | Global filter is used if empty/blank/not set                        |

### Identity
| Item     | Description                                                                                       |
|----------|---------------------------------------------------------------------------------------------------|
| `filter` | Specific user filter applied during identity search. Global filter is used if empty/blank/not set | 
| `medium` | Namespace to overwrite generated queries from the list of attributes for each 3PID medium         |
