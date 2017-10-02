mxisd - Federated Matrix Identity Server Daemon
-----
![Travis-CI build status](https://travis-ci.org/kamax-io/mxisd.svg?branch=master)  

- [Overview](#overview)
- [Features](#features)
- [Why use mxisd](#why-use-mxisd)
- [Quick start](#quick-start)
- [Support](#support)
- [Contribute](#contribute)
- [FAQ](#faq)
- [Contact](#contact)

# Overview
mxisd is a Federated Matrix Identity server for self-hosted Matrix infrastructures with enhanced features.
  
It is specifically designed to connect to an Identity store (AD/Samba/LDAP, SQL Database, Web services/application, ...)
and ease the integration of the Matrix ecosystem with an existing infrastructure, or to build a new one using lasting
tools.

The core principle of mxisd is to map between Matrix IDs and 3PIDs (Thrid-party Identifiers) for the Homeserver and its
users. 3PIDs can be anything that identify a user, like:
- Full name
- Email address
- Phone number
- Employee number
- Skype/Live ID
- Twitter handle
- Facebook ID
- ...

mxisd is an enhanced Identity service, which implements the [Matrix Identity service API](https://matrix.org/docs/spec/identity_service/unstable.html)
but also several other features that greatly enhance user experience within Matrix.

mxisd is the one stop shop for anything regarding Authentication, Directory and Identity management in Matrix built as a
single coherent product.

# Features
As a [regular Matrix Identity service](docs/features/identity.md):
- Search for people by 3PID using its own Identity stores (LDAP, SQL, etc.)
- Invite people to rooms by 3PID using its own Identity stores, with [notifications](docs/README.md)
to the invitee (Email, SMS, etc.)
- Allow users to add 3PIDs to their settings/profile

As an enhanced Identity service:
- Use a recursive lookup mechanism when searching and inviting people by 3PID, allowing to fetch data from:
  - Own Identity store
  - Federated Identity servers, if applicable to the 3PID
  - Arbitrary Identity servers
  - Central Matrix Identity servers
- [Extensive control of where 3PIDs are transmited](docs/sessions/3pid.md), so they are not leaked publicly by users
- [Authentication support](docs/features/authentication.md) for [synapse](https://github.com/matrix-org/synapse) via the
[REST auth module](https://github.com/kamax-io/matrix-synapse-rest-auth)
- [Directory integration](docs/features/directory-users.md) which allows you to search for users within your
organisation, even without prior Matrix contact
- [Auto-fill of user profile](docs/features/authentication.md) (Display name, 3PIDs) via the
[REST auth module](https://github.com/kamax-io/matrix-synapse-rest-auth)

# Why use mxisd
- Use your existing Identity store, do not duplicate information
- Auto-fill user profiles with relevant information
- As an organisation, stay in control of 3PIDs so they are not published to the central Matrix.org servers where they
currently **cannot be removed**
- Users can directly find each other using whatever attribute is relevant within your Identity store
- Federate your Identity lookups so you can discover others and/or others can discover you, all with extensive ACLs

# Quick Start
1. [Preparation](#preparation)
2. [Install](#install)
3. [Configure](#configure)
4. [Integrate](#integrate)
5. [Validate](#validate)

Following these quick start instructions, you will have a basic setup that can perform recursive/federated lookups and
talk to the central Matrix.org Identity service.  
This will be a good ground work for further integration with your existing Identity stores.

## Preparation
You will need:
- Homeserver
- Reverse proxy with regular TLS/SSL certificate (Let's encrypt) for your mxisd domain

As synapse requires an HTTPS connection when talking to an Identity service, a reverse proxy is required as mxisd does
not support HTTPS listener at this time.

For maximum integration, it is best to have your Homeserver and mxisd reachable via the same hostname.  
You can also use a dedicated domain for mxisd, but will not have access to some features.

Be aware of a [NAT/Reverse proxy gotcha](https://github.com/kamax-io/mxisd/wiki/Gotchas#nating) if you use the same
hostname.

The following Quick Start guide assumes you will host the Homeserver and mxisd under the same hostname.  
If you would like a high-level view of the infrastructure and how each feature is integrated, see the
[dedicated document](docs/architecture.md)

## Install
Install via:
- [Debian package](docs/install/debian.md)
- [Docker image](docs/install/docker.md)
- [Sources](docs/build.md)

See the [Latest release](https://github.com/kamax-io/mxisd/releases/latest) for links to each.

## Configure
Create/edit a minimal configuration (see installer doc for the location):
```
matrix.domain: 'MyMatrixDomain.org'
key.path: '/path/to/signing.key.file'
storage.provider.sqlite.database: '/path/to/mxisd.db'
```  
- `matrix.domain` should be set to your Homeserver domain
- `key.path` will store the signing keys, which must be kept safe!
- `storage.provider.sqlite.database` is the location of the SQLite Database file which will hold state (invites, etc.)

If your HS/mxisd hostname is not the same as your Matrix domain, configure `server.name`.  
Complete configuration guide is available [here](docs/configure.md).

## Integrate
For an overview of a typical mxisd infrastructure, see the [dedicated document](docs/architecture.md)
### Reverse proxy
#### Apache2
In the VirtualHost handling the domain with SSL, add the following line and replace `0.0.0.0` by the right address/host.  
**This line MUST be present before the one for the homeserver!**
```
ProxyPass /_matrix/identity/ http://0.0.0.0:8090/_matrix/identity/
```

Typical VirtualHost configuration would be:
```
<VirtualHost *:443>
    ServerName example.org
    
    ...
    
    ProxyPreserveHost on
    ProxyPass /_matrix/identity/ http://10.1.2.3:8090/_matrix/identity/
    ProxyPass /_matrix/ http://10.1.2.3:8008/_matrix/
</VirtualHost>
```

### Synapse
Add your mxisd domain into the `homeserver.yaml` at `trusted_third_party_id_servers` and restart synapse.  
In a typical configuration, you would end up with something similair to:
```
trusted_third_party_id_servers:
    - matrix.org
    - vector.im
    - example.org
```
It is recommended to remove `matrix.org` and `vector.im` so only your own Identity server is allowed by synapse. 

### Federation and network discovery
See the [dedicated document](docs/features/federation.md).

## Validate
Log in using your Matrix client and set `https://example.org` as your Identity server URL, replacing `example.org` by
the relevant hostname which you configured in your reverse proxy.  
Invite `mxisd-lookup-test@kamax.io` to a room, which should be turned into a Matrix invite to `@mxisd-lookup-test:kamax.io`.  
**NOTE:** you might not see a Matrix suggestion for the e-mail address, which is normal. Still proceed with the invite.
  
If it worked, it means you are up and running and can enjoy mxisd in its basic mode! Congratulations!  
If it did not work, [get in touch](#support) and we'll do our best to get you started.

You can now integrate mxisd further with your infrastructure using the various [features](docs/README.md) guides.

# Support
## Community
If you need help, want to report a bug or just say hi, you can reach us on Matrix at 
[#mxisd:kamax.io](https://matrix.to/#/#mxisd:kamax.io) or [directly peek anonymously](https://view.matrix.org/room/!NPRUEisLjcaMtHIzDr:kamax.io/).
For more high-level discussion about the Identity Server architecture/API, go to 
[#matrix-identity:matrix.org](https://matrix.to/#/#matrix-identity:matrix.org)

## Professional
If you would prefer professional support/custom development for mxisd and/or for Matrix in general, including other open source technologies/products, 
please visit [our website](https://www.kamax.io/) to get in touch with us and get a quote.

We offer affordable monthly/yearly support plans for mxisd, synapse or your full Matrix infrastructure.

# Contribute
First and foremost, the best way to contribute is to use mxisd and tell us about it!  
We would love to hear about your experience and get your feedback on how to make it an awesome product. 

You can contribute as a community member by:
- Opening issues for any weird behaviour or bug. mxisd should feel natural, let us know if it does not!
- Helping us improve the documentation: tell us what is good or not good (in an issue or in Matrix), or make a PR with
changes you feel improve the doc.
- Contribute code directly: we love contributors! All your contributions will be licensed under AGPLv3.
- Donate! any donation is welcome, regardless how small or big. This will directly be used for the fixed costs and
developer time.

You can contribute as an organisation/corporation by:
- Get a [support contract](#support-professional). This is the best way you can help us as it ensures mxisd is
maintained regularly and you get direct access to the support team.
- Sponsoring new features or bug fixes. [Get in touch](#contact) so we can discuss it further.

# FAQ
### Do I need to use mxisd if I run a Homeserver?
No, but it is recommended, even if you don't use any backends or integration.

mxisd in its default configuration will use federation and involve the central Matrix.org Identity servers when
performing queries, giving you access to at least the same information as if you were not running it.

It will also give your users a choice to make their 3PIDs available publicly, ensuring they are made aware of the
privacy consequences, which is not the case with the central Matrix.org servers.

So mxisd is like your gatekeeper and guardian angel. It does not change what you already know, just adds some nice
simple features on top of it.

### I already use the synapse LDAP3 auth provider, why should I care about mxisd?
The [synapse LDAP3 auth provider](https://github.com/matrix-org/matrix-synapse-ldap3) only handles on specific flow:
validate credentials at login.

It does not:
- Auto-provision user profiles
- Integrate with Identity management
- Integrate with Directory searches
- Protect you against the username case sensitivites issues in synapse

mxisd is a replacement and enhancement of it, and also offers coherent results in all areas, which LDAP3 auth provider
does not.

### I saw that sydent is the official Identity server implemenation of the Matrix team, I should use that!
You can, but [sydent](https://github.com/matrix-org/sydent):
- [should not be used and/or self-hosted](https://github.com/matrix-org/sydent/issues/22)
- is not meant to be linked to a specific Homeserver / domain
- cannot handle federation or proxy lookups, effectively isolating your users from the rest of the network
- forces you to duplicate all your identity data, so people can be found by 3PIDs
- forces users to enter all their emails and phone numbers manually in their profile

So really, you should go with mxisd.

### I'm not sure I understand what an "Identity server" is supposed to be or do
The current Identity service API is more a placeholder, as the Matrix devs did not have time so far to really work on
what they want to do with that part of the ecosystem. Therefore, "Identity" is a misleading word currently.  
Given the scope of the current Identity Service API, it would be best called "Invitation service".

Because the current scope is so limited and no integration is done with the Homeserver, there was a big lack of features
for groups/corporations/organisation. This is where mxisd comes in.

mxisd implements the Identity Service API and also a set of features which are expected by regular users, truly living
up to its "Identity server" name.

### So mxisd is just a big hack! I don't want to use non-official features!
mxisd primary concern is to always be compatible with the Matrix ecosystem and the Identity service API.  
Whenever the API will be updated and/or enhanced, mxisd will follow, remaining 100% compatible with the ecosystem.

We also directly talk with the Matrix developers to ensure all features we implement have their approval, and that we
are in line with their vision of Identity management within the Matrix ecosystem.

Therefore, using mxisd is a safe choice. It will be like using the central Matrix.org Identity servers, yet not closing
the door to very nice enhancements and integrations.

### Should I use mxisd if I don't host my own Homeserver?
No

# Contact
Get in touch via:
- Matrix at [#mxisd:kamax.io](https://matrix.to/#/#mxisd:kamax.io)
- Email, see our website: [Kamax.io](https://www.kamax.io)
