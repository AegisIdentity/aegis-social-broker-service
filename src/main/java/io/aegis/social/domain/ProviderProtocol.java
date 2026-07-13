package io.aegis.social.domain;

/**
 * How a registered identity provider is brokered.
 *
 * <ul>
 *   <li>{@code OIDC} — OpenID Connect with discovery (Google, Microsoft Entra ID, Apple, generic).</li>
 *   <li>{@code OAUTH2} — plain OAuth2 with explicit endpoints and a userinfo API (GitHub).</li>
 *   <li>{@code SAML} — SAML 2.0 relying-party (corporate federation).</li>
 * </ul>
 */
public enum ProviderProtocol {
    OIDC,
    OAUTH2,
    SAML
}
