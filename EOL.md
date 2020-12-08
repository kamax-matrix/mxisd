# End of Life notice 

**This project will be merged with [Gridepo](https://gitlab.com/kamax-io/grid/gridepo), a dual-stack Grid/Matrix server supporting several APIs.**

mxisd was originally created to [help safeguard privacy in Matrix](https://github.com/kamax-matrix/mxisd/wiki/mxisd-and-your-privacy#mxisd-and-your-privacy), and act as a reference for self-hosted infrastructures that required a certain level of compliance, mainly for [EU GDPR](https://eugdpr.org/).

[Matrix 1.0 has been released](https://matrix.org/blog/2019/06/11/introducing-matrix-1-0-and-the-matrix-org-foundation) which sets a milestone: it sets the level of what is acceptable in terms of privacy and security for the protocol and its reference implementations moving forward.  

We believe the purpose of the mxisd project has now been accomplished - offering an alternative and raising awareness - and we do not see any value going forward with it anymore:
- The place of Identity servers in the stack has always been misunderstood and Identity servers [are still considered optional](https://matrix.org/faq#what-is-an-identity-server%3F). Matrix.org also [recommends to not self-host them](https://matrix.org/faq#can-i-run-my-own-identity-server%3F), or even to not use them.
  and most likely will not in the future.
- The change of direction to no longer rely on Identity servers for key behaviours
  (e.g. [password resets](https://github.com/matrix-org/synapse/pull/5377)) means this project will not maintain its value in the long term.
