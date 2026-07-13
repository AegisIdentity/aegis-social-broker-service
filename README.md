# aegis-social-broker-service

Inbound federation: social login + corporate SAML/OIDC brokering.

**Maturity: scaffold.** Buildable, secured resource-server skeleton — health endpoint, a protected
placeholder API, and the shared `aegis-security-commons` hardening baseline. Feature work goes here;
the intended contract is in
[`aegis-platform-docs/architecture/SERVICE-CATALOG.md`](../aegis-platform-docs/architecture/SERVICE-CATALOG.md).

- Port: `9105` · Required scope for `/api/**`: `social:admin`
- Build: `./mvnw verify` (needs `aegis-platform-parent` + `aegis-platform-commons` installed to `~/.m2` first)
