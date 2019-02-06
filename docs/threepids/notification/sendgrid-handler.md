# SendGrid Notification handler
> **WARNING:** This section is incomplete and may be misleading. Contact us if guidance is needed.

Enable with:
```yaml
notification:
  handler:
    email: 'sendgrid'
```

Available Configuration keys:
```yaml
notification:
  handlers:
    sendgrid:
      api:
        key: <API key>
      identity:
        from: <Sender email address>
        name: <Sender name>
      templates:
        invite:
          subject: <Subject of the email notification sent for room invites>
          body:
            text: <Path to file containing the raw text part of the email. Do not set to not use one>
            html: <Path to file containing the HTML part of the email. Do not set to not use one>
        session:
          validation:
            subject: <Subject of the email notification sent for 3PID sessions>
            body:
              text: <Path to file containing the raw text part of the email. Do not set to not use one>
              html: <Path to file containing the HTML part of the email. Do not set to not use one>
          unbind:
            fraudulent:
              subject: <Subject of the email notification sent for potentially fraudulent 3PID unbinds>
              body:
                text: <Path to file containing the raw text part of the email. Do not set to not use one>
                html: <Path to file containing the raw text part of the email. Do not set to not use one>
``` 
