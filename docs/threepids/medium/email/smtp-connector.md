# Email notifications - SMTP connector
Connector ID: `smtp`

## Configuration
```yaml
threepid:
  medium:
    email:
      identity:
        from: 'identityServerEmail@example.org'
        name: 'My Identity Server'
      connectors:
        smtp:
          host: 'smtpHostname'
          tls: 1 # 0 = no STARTLS, 1 = try, 2 = force, 3 = TLS/SSL
          port: 587 # Set appropriate value depending on your TLS setting
          login: 'smtpLogin'
          password: 'smtpPassword'
```
