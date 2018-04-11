mxisd - Federated Matrix Identity Server
----------------------------------------
![Travis-CI build status](https://travis-ci.org/kamax-io/mxisd.svg?branch=master)  

- [Overview](#overview)
- [Features](#features)
- [Use cases](#use-cases)
- [Getting Started](#getting-started)
- [Support](#support)
- [Contribute](#contribute)
- [FAQ](#faq)
- [Contact](#contact)

# Overview
mxisd is a Federated Matrix Identity server for self-hosted Matrix infrastructures with [enhanced features](#features).
It is an enhanced Identity service, which implements the [Matrix Identity service API](https://kamax.io/matrix/api/identity_service/unstable.html)
but also several [other features](#features) that greatly enhance user experience within Matrix.
It is the one stop shop for anything regarding Authentication, Directory and Identity management in Matrix built as a
single coherent product.
  
mxisd is specifically designed to connect to an existing on-remise Identity store (AD/Samba/LDAP, SQL Database,
Web services/app, etc.) and ease the integration of a Matrix infrastructure within an existing one.

The core principle of mxisd is to map between Matrix IDs and 3PIDs (Third-Party IDentifiers) for the Homeserver and its
users. 3PIDs can be anything that uniquely and globally identify a user, like:
- Email address
- Phone number
- Skype/Live ID
- Twitter handle
- Facebook ID

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
organisation, even without prior contact within Matrix
- [Auto-fill of user profile](docs/features/authentication.md) (Display name, 3PIDs)

# Use cases
- Use your existing Identity store, do not duplicate information
- Auto-fill user profiles with relevant information
- As an organisation, stay in control of 3PIDs so they are not published to the central Matrix.org servers where they
currently **cannot be removed**
- Users can directly find each other using whatever attribute is relevant within your Identity store
- Federate your Identity lookups so you can discover others and/or others can discover you, all with extensive ACLs

# Getting started
See the [dedicated document](docs/getting-started.md)

# Support
## Community
If you need help, want to report a bug or just say hi, you can reach us on Matrix at 
[#mxisd:kamax.io](https://matrix.to/#/#mxisd:kamax.io) or
[directly peek anonymously](https://view.matrix.org/room/!NPRUEisLjcaMtHIzDr:kamax.io/).
For more high-level discussion about the Identity Server architecture/API, go to 
[#matrix-identity:matrix.org](https://matrix.to/#/#matrix-identity:matrix.org)

## Commercial
If you would prefer professional support/custom development for mxisd and/or for Matrix in general, including other open
source technologies/products, please visit [our website](https://www.kamax.io/) to get in touch with us and get a quote.

# Contribute
First and foremost, the best way to contribute is to use mxisd and tell us about it!  
We would love to hear about your experience and get your feedback on how to make it an awesome product. 

You can contribute as a community member by:
- Opening issues for any weird behaviour or bug. mxisd should feel natural, let us know if it does not!
- Helping us improve the documentation: tell us what is good or not good (in an issue or in Matrix), or make a PR with
changes you feel improve the doc.
- Contribute code directly: we love contributors! All your contributions will be licensed under AGPLv3.
- [Donate!](https://liberapay.com/maximusdor/) Any donation is welcome, regardless how small or big, and will directly
be used for the fixed costs and developer time of mxisd.

You can contribute as an organisation/corporation by:
- Get a [support contract](#support-commercial). This is the best way you can help us as it ensures mxisd is
maintained regularly and you get direct access to the support team.
- Sponsoring new features or bug fixes. [Get in touch](#contact) so we can discuss it further.

# FAQ
See the [dedicated document](docs/faq.md)

# Contact
Get in touch via:
- Matrix: [#mxisd:kamax.io](https://matrix.to/#/#mxisd:kamax.io)
- Email: see our website: [Kamax.io](https://www.kamax.io)
