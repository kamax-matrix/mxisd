# FAQ
### Do I need to use mxisd if I run a Homeserver?
No, but it is recommended, even if you don't use any backends or integration.

In its default configuration, mxisd will talk to the central Matrix Identity servers and use other federated public
servers when performing queries, giving you access to at least the same information as if you were not running it.

It will also give your users a choice to make their 3PIDs available publicly, ensuring they are made aware of the
privacy consequences, which is not the case with the central Matrix.org servers.

So mxisd is like your gatekeeper and guardian angel. It does not change what you already know, just adds some nice
simple features on top of it.

### I already use the synapse LDAP3 auth provider, why should I care about mxisd?
The [synapse LDAP3 auth provider](https://github.com/matrix-org/matrix-synapse-ldap3) is not longer maintained and
only handles on specific flow: validate credentials at login.

It does not:
- Auto-provision user profiles
- Integrate with Identity management
- Integrate with Directory searches
- Protect you against the username case sensitivites issues in synapse

mxisd is a replacement and enhancement of it, offering coherent results in all areas, which LDAP3 auth provider
does not.

### Sydent is the official Identity server implementation of the Matrix team, why not use that?
You can, but [sydent](https://github.com/matrix-org/sydent):
- [should not be used and/or self-hosted](https://github.com/matrix-org/sydent/issues/22)
- is not meant to be linked to a specific Homeserver / domain
- cannot handle federation or proxy lookups, effectively isolating your users from the rest of the network
- forces you to duplicate all your identity data, so people can be found by 3PIDs
- forces users to enter all their emails and phone numbers manually in their profile

So really, you should go with mxisd.

### Will I loose access to the central Matrix.org/Vector.im Identity data if I use mxisd?
In its default configuration, mxisd act as a proxy to Matrix.org/Vector.im. You will have access to the same data and
behaviour than if you were using them directly. There is no downside in using mxisd with the default configuration.

mxisd can also be configured not to talk to the central Identity servers if you wish.

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
