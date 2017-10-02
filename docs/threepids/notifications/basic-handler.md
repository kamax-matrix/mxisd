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
```
notification:
  handler:
    email: 'raw'
```

**WARNING:** Will be consolidated soon, prone to breaking changes.  
Structure and default values:
```
threepid:
  medium:
    email:
      identity:
        from: ''
        name: ''
      connector: 'smtp'
      generator: 'template'
      connectors:
        smtp:
          host: ''
          port: 587
          tls: 1
          login: ''
          password: ''
      generators:
        template:
          invite: 'classpath:threepids/email/invite-template.eml'
          session:
            validation:
              local: 'classpath:threepids/email/validate-local-template.eml'
              remote: 'classpath:threepids/email/validate-remote-template.eml'
    msisdn:
      connector: 'twilio'
      generator: 'template'
      connectors:
        twilio:
          accountSid: ''
          authToken: ''
          number: ''
      generators:
        template:
          invite: 'classpath:threepids/sms/invite-template.txt'
          session:
            validation:
              local: 'classpath:threepids/sms/validate-local-template.txt'
              remote: 'classpath:threepids/sms/validate-remote-template.txt'
```
