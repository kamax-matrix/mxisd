# End of Life notice 

**This project will be merged with [Gridepo](https://gitlab.com/kamax-io/grid/gridepo), a dual-stack Grid/Matrix server supporting several APIs.**

mxisd was originally created to [help safeguard privacy in Matrix](https://github.com/kamax-matrix/mxisd/wiki/mxisd-and-your-privacy#mxisd-and-your-privacy), and act as a reference for self-hosted infrastructures that required a certain level of compliance, mainly for [EU GDPR](https://eugdpr.org/).

[Matrix 1.0 has been released](https://matrix.org/blog/2019/06/11/introducing-matrix-1-0-and-the-matrix-org-foundation) which sets a milestone: it sets the level of what is acceptable in terms of privacy and security for the protocol and its reference implementations moving forward.  
With the help of the community, we wrote a research paper to highlight the current state of privacy: [Notes on privacy and data collection of Matrix.org](https://gist.github.com/maxidorius/5736fd09c9194b7a6dc03b6b8d7220d0).

We believe the purpose of the mxisd project has now been accomplished - offering an alternative and raising awareness - and we do not see any value going forward with it anymore:
- The place of Identity servers in the stack has always been misunderstood and Identity servers [are still considered optional](https://matrix.org/faq#what-is-an-identity-server%3F). Matrix.org also [recommends to not self-host them](https://matrix.org/faq#can-i-run-my-own-identity-server%3F), or even to not use them.
- The extent of the privacy leaks in [our research paper](https://gist.github.com/maxidorius/5736fd09c9194b7a6dc03b6b8d7220d0) shows that mxisd cannot significantly make a difference,
  and most likely will not in the future.
- The Matrix.org team [does not share the same views on Privacy and GDPR](https://gist.github.com/maxidorius/5736fd09c9194b7a6dc03b6b8d7220d0#gistcomment-2943323),
  and has never taken significant interest into the project or our contributions to the Identity Server specification.
- The recent change of direction to no longer rely on Identity servers for key behaviours
  (e.g. [password resets](https://github.com/matrix-org/synapse/pull/5377)) means this project will not maintain its value in the long term.

Commercial support is still available for bug and security fixes. See [our website](https://www.kamax.io/) for contact information.

---

**See you in [The Grid](https://gitlab.com/thegridprotocol/home/blob/master/README.md#the-grid) for the next privacy-focused protocol!**