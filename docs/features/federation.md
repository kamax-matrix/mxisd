# Identity service Federation
To allow other federated Identity Server to reach yours, the same algorithm used for Homeservers takes place:
1. Check for the appropriate DNS SRV record
2. If not found, use the base domain
 
If your Identity Server public hostname does not match your Matrix domain, configure the following DNS SRV entry 
and replace `matrix.example.com` by your Identity server public hostname - **Make sure to end with a final dot!**
```
_matrix-identity._tcp.example.com. 3600 IN SRV 10 0 443 matrix.example.com.
``` 
This would only apply for 3PID that are DNS-based, like e-mails. For anything else, like phone numbers, no federation 
is currently possible.  

The port must be HTTPS capable. Typically, TCP port `8090` of mxisd should be behind a reverse proxy which does HTTPS.
See the [main README integration section](../README.md#integration) for more details.
