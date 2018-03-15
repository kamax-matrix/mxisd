# Wordpress
This Identity store allows you to connect with accounts registered on your Wordpress install using the [REST API](https://developer.wordpress.org/rest-api/).

This Identity store supports:
- Authentication
- Directory
- Identity

## Requirements
- [Wordpress](https://wordpress.org/download/) >= 4.4
- [JWT Auth plugin for REST API](https://wordpress.org/plugins/jwt-authentication-for-wp-rest-api/)

## Configuration
### Wordpress
Set a JWT secret into `wp-config.php` like so:
```
define('JWT_AUTH_SECRET_KEY', 'your-top-secret-key');
```

### mxisd
Enable in the configuration:
```
wordpress.enabled: true
```

TBC