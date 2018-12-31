# Basic Notification handler
Basic notification handler which uses two components:
- Content generator, to produce the notifications
- Connectors to send the notification content

This handler can be used with the 3PID types:
- `email`
- `msisdn` (Phone numbers)

## Generators
- [Template](template-generator.md)
## Connectors
- Email
  - [SMTP](../medium/email/smtp-connector.md)
- SMS
  - [Twilio](../medium/msisdn/twilio-connector.md)

## Configuration
Enabled by default or with:
```yaml
notification:
  handler:
    email: 'raw'
```

**WARNING:** Will be consolidated soon, prone to breaking changes.  
Structure and default values:
```yaml
threepid:
  medium:
    email:
      identity:
        from: ''
        name: ''
      connector: 'smtp'
      generator: 'template'
    msisdn:
      connector: 'twilio'
      generator: 'template'
```
