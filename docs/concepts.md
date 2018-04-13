# Concepts
- [Matrix](#matrix)
- [mxisd](#mxisd)

## Matrix
The following concepts are part of the Matrix ecosystem and specification.

### 3PID
`3PID` stands for Third-Party Identifier.  
It is also commonly written:
- `3pid`
- `tpid`

A 3PID is a globally unique canonical identifier which is made of:
- Medium, which describes what network it belongs to (Email, Phone, Twitter, Discord, etc.)
- Address, the actual value people typically use on a daily basis.

mxisd core mission is to map those identifiers to Matrix User IDs.

### Homeserver
Where a user **account and data** are stored.

### Identity server
An Identity server:
- Does lookup of 3PIDs to User Matrix IDs.
- Does validate 3PIDs ownership, typically by sending a code that the user has to enter in an application/on a website
- Does send notifications about room invites where no Matrix User ID could be found for the invitee.

An Identity server:
- **DOES NOT** store user accounts
- **DOES NOT** store user sessions
- **DOES NOT** allow migration of user account and/or data between homeservers 

### 3PID session
The fact to validate a 3PID (email, phone number, etc.) via the introduction of a token which was sent to the 3PID address.

## mxisd
The following concepts are specific to mxisd

### Identity store
Where your user accounts and 3PID mappings are stored.

**mxisd DOES NOT STORE** user accounts or 3PID mappings**
