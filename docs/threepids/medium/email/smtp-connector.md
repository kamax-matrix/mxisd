# Email notifications - SMTP connector
Connector ID: `smtp`

Example configuration:
```
threepid:
  medium:
    email:
      identity:
        from: 'identityServerEmail@example.org'
        name: 'My Identity Server'
      connectors:
        smtp:
          host: 'smtpHostname'
          port: 587
          tls: 1 # 0 = no STARTLS, 1 = try, 2 = force
          login: 'smtpLogin'
          password: 'smtpPassword'
```
