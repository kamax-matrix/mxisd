# Wordpress
This Identity store allows you to use user accounts registered on your Wordpress setup.  
Two types of connections are required for full support:
- [REST API](https://developer.wordpress.org/rest-api/) with JWT authentication
- Direct SQL access

This Identity store supports:
- Authentication
- Directory
- Identity

## Requirements
- [Wordpress](https://wordpress.org/download/) >= 4.4
- Permalink structure set to `Post Name`
- [JWT Auth plugin for REST API](https://wordpress.org/plugins/jwt-authentication-for-wp-rest-api/)
- SQL Credentials to the Wordpress Database

## Configuration
### Wordpress
#### JWT Auth
Set a JWT secret into `wp-config.php` like so:
```
define('JWT_AUTH_SECRET_KEY', 'your-top-secret-key');
```
`your-top-secret-key` should be set to a randomly generated value which is kept secret.

#### Rewrite of `index.php`
Wordpress is normally configured with rewrite of `index.php` so it does not appear in URLs.  
If this is not the case for your installation, the mxisd URL will need to be appended with `/index.php`

### mxisd
Enable in the configuration:
```
wordpress.enabled: true
```
Configure the URL to your Wordpress installation - see above about added `/index.php`:
```
wordpress.rest.base: 'http://localhost:8080'
```
Configure the SQL connection to your Wordpress database:
```
wordpress.sql.connection: '//127.0.0.1/wordpress?user=root&password=example'
```

---

By default, MySQL database is expected. If you use another database, use:
```
wordpress.sql.type: 'jdbc-scheme'
```
With possible values:
- `mysql`
- `mariadb`
- `postgresql`
- `sqlite`
