# Google Firebase
https://firebase.google.com/

## Requirements
This backend requires a suitable Matrix client capable of performing Firebase authentication and passing the following
information:
- Firebase User ID as Matrix username
- Firebase token as Matrix password

If your client is Riot, you will need a custom version.

## Configuration
To be completed. For now, see default structure and values:
```
firebase:
  enabled: false
  credentials: '/path/to/firebase/credentials.json'
  database: 'https://my-project.firebaseio.com/'
```
