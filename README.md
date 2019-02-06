mxisd - Federated Matrix Identity Server
----------------------------------------
![Travis-CI build status](https://travis-ci.org/kamax-matrix/mxisd.svg?branch=master)  

- [Overview](#overview)
- [Features](#features)
- [Use cases](#use-cases)
- [Getting Started](#getting-started)
- [Support](#support)
- [Contribute](#contribute)
- [Powered by mxisd](#powered-by-mxisd)
- [FAQ](#faq)
- [Contact](#contact)

# Overview
mxisd is a Federated Matrix Identity server for self-hosted Matrix infrastructures with [enhanced features](#features).
As an enhanced Identity service, it implements the [Identity service API](https://matrix.org/docs/spec/identity_service/r0.1.0.html)
and several [extra features](#features) that greatly enhance user experience within Matrix.
It is the one stop shop for anything regarding Authentication, Directory and Identity management in Matrix built in a
single coherent product.
  
mxisd is specifically designed to connect to an existing on-premise Identity store (AD/Samba/LDAP, SQL Database,
Web services/app, etc.) and ease the integration of a Matrix infrastructure within an existing one.

The core principle of mxisd is to map between Matrix IDs and 3PIDs (Third-Party IDentifiers) for the Homeserver and its
users. 3PIDs can be anything that uniquely and globally identify a user, like:
- Email address
- Phone number
- Skype/Live ID
- Twitter handle
- Facebook ID

If you are unfamiliar with the Identity vocabulary and concepts in Matrix, **please read this [introduction](docs/concepts.md)**.

# Features
[Identity](docs/features/identity.md): As a [regular Matrix Identity service](https://matrix.org/docs/spec/identity_service/r0.1.0.html#general-principles):
- Search for people by 3PID using its own Identity stores
  ([Spec](https://matrix.org/docs/spec/identity_service/r0.1.0.html#association-lookup))
- Invite people to rooms by 3PID using its own Identity stores, with notifications to the invitee (Email, SMS, etc.)
  ([Spec](https://matrix.org/docs/spec/identity_service/r0.1.0.html#post-matrix-identity-api-v1-store-invite))
- Allow users to add 3PIDs to their settings/profile
  ([Spec](https://matrix.org/docs/spec/identity_service/r0.1.0.html#establishing-associations))
- Register accounts on your Homeserver with 3PIDs
  ([Spec](https://matrix.org/docs/spec/identity_service/r0.1.0.html#establishing-associations))

As an enhanced Identity service:
- [Federation](docs/features/federation.md): Use a recursive lookup mechanism when searching and inviting people by 3PID,
  allowing to fetch data from:
  - Own Identity store(s)
  - Federated Identity servers, if applicable to the 3PID
  - Arbitrary Identity servers
  - Central Matrix Identity servers
- [Session Control](docs/threepids/session/session.md): Extensive control of where 3PIDs are transmitted so they are not
  leaked publicly by users
- [Authentication](docs/features/authentication.md): Use your Identity stores to perform authentication in [synapse](https://github.com/matrix-org/synapse)
  via the [REST password provider](https://github.com/kamax-io/matrix-synapse-rest-auth)
- [Directory search](docs/features/directory.md) which allows you to search for users within your organisation,
  even without prior contact within Matrix using arbitrary search terms
- [Auto-fill of user profile](docs/features/authentication.md#profile-auto-fill) (Display name, 3PIDs)
- [Bridge Integration](docs/features/bridge-integration.md): Automatically bridge users without a published Matrix ID

# Use cases
- Use your existing Identity stores, do not duplicate your users information
- Auto-fill user profiles with relevant information
- As an organisation, stay in control of your data so it is not published to other servers by default where they
  currently **cannot be removed**
- Users can directly find each other using whatever attribute is relevant within your Identity store
- Federate your Identity server so you can discover others and/or others can discover you

# Getting started
See the [dedicated document](docs/getting-started.md)

# Support
## Community
Over Matrix: [#mxisd:kamax.io](https://matrix.to/#/#mxisd:kamax.io) ([Preview](https://view.matrix.org/room/!NPRUEisLjcaMtHIzDr:kamax.io/))

For more high-level discussion about the Identity Server architecture/API, go to  [#matrix-identity:kamax.io](https://matrix.to/#/#matrix-identity:kamax.io)

## Commercial
If you would prefer professional support/custom development for mxisd and/or for Matrix in general, including other open
source technologies/products:
- Visit our [website](https://www.kamax.io/) to get in touch with us and get a quote.
- Come in our general Matrix room: [#kamax-matrix:kamax.io](https://matrix.to/#/#kamax-matrix:kamax.io)

# Contribute 
You can contribute as a community member by:
- Giving us feedback about your usage of mxisd, even if it seems unimportant or if all is working well!
- Opening issues for any weird behaviour or bug. mxisd should feel natural, let us know if it does not!
- Helping us improve the documentation: tell us what is good or not good (in an issue or in Matrix), or make a PR with
changes you feel improve the doc.
- Contribute code directly: we love contributors! All your contributions will be licensed under AGPLv3.
- [Donate!](https://liberapay.com/maximusdor/) Any donation is welcome, regardless how small or big, and will directly
be used for the fixed costs and developer time of mxisd.

You can contribute as an organisation/corporation by:
- Get a [support contract](#commercial). This is the best way you can help us as it ensures mxisd is
maintained regularly and you get direct access to the support team.
- Sponsoring new features or bug fixes. [Get in touch](#contact) so we can discuss it further.

# Powered by mxisd
The following projects use mxisd under the hood for some or all their features. Check them out!
- [matrix-docker-ansible-deploy](https://github.com/spantaleev/matrix-docker-ansible-deploy)
- [matrix-register-bot](https://github.com/krombel/matrix-register-bot)

# FAQ
See the [dedicated document](docs/faq.md)

# Contact
Get in touch via:
- Matrix: [#mxisd:kamax.io](https://matrix.to/#/#mxisd:kamax.io)
- Email: see our website: [Kamax.io](https://www.kamax.io)
