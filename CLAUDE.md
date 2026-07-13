# aegis-social-broker-service — working notes

Inbound federation: per-tenant social/OIDC/SAML identity-provider registry + brokering.
**Maturity: functional (config).** Port 9105. Postgres (`aegis_social`), JPA.

## What this is
A Spring Boot 4.1 / Java 21 OAuth2 resource server. Package root `io.aegis.social`.
- `domain/IdentityProvider` — per-tenant provider config (unique alias per tenant). `clientSecret` is
  stored but **write-only** (never returned by the API).
- `service/ProviderCatalog` + `ProviderPreset` — presets for GOOGLE/ENTRA/APPLE/GITHUB/OIDC/SAML
  (endpoints, scopes, protocol, required extra fields). Entra issuer is templated from `providerDirectory`.
- `service/IdentityProviderService` — tenant-scoped CRUD (tenant from the token, never the body).
- `web/IdentityProviderController` — `/api/v1/identity-providers` (+ `/catalog`), gated `SCOPE_idp:admin`.
- `config/ResourceServerJwtConfig` — split-horizon JWT decoder (in-network JWKS + issuer allowlist);
  do NOT revert to a single `issuer-uri`. See [[aegis-issuer-split-horizon]].

Security lives in `SecurityConfig` (uses `io.aegis.commons.security.SecurityHardening`).

## Next (not built yet)
The live federation **runtime** (redirect to provider → callback → JIT-provision → Aegis token) is
Phase 2, executed by the authorization-server against these configs. SAML runtime is Phase 3.

## Non-negotiables (do not regress)
- Default-deny: every new endpoint stays denied until an explicit `authorizeHttpRequests` rule + a
  **negative test** ("no token -> 401", "wrong scope -> 403") is added.
- Keep the shared hardening baseline applied. Validate JWTs against the authorization-server issuer.
- All persistent data must be **tenant-scoped** (see ARCHITECTURE.md §5). No cross-tenant reads.

## Build / test
`./mvnw verify` — resolves `aegis-platform-parent` and `aegis-security-commons` from `~/.m2`
(build `aegis-platform-bom` and `aegis-platform-commons` first).

## Next steps
Implement the endpoints in SERVICE-CATALOG.md, add JPA + Testcontainers integration tests following
the pattern in `aegis-identity-service`, and wire domain events to `aegis-audit-commons`.
