# SMS notifications - Twilio connector
Enabled by default.

Connector ID: `twilio`

## Configuration
```yaml
threepid:
  medium:
    msisdn:
      connectors:
        twilio:
          accountSid: 'myAccountSid'
          authToken: 'myAuthToken'
          number: '+123456789'
```
