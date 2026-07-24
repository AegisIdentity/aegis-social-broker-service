package io.aegis.social.domain;

import io.aegis.social.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * A per-tenant registered identity provider (social/OIDC/SAML). Always scoped to a {@code tenantId};
 * the {@code alias} is unique per tenant and is what appears in the "Sign in with …" button and the
 * federation callback path. The {@code clientSecret} is stored (AES-256-GCM encrypted at rest, M-svc-2)
 * but never returned by the public API.
 *
 * <p>Fields are a superset covering all protocols; which are populated depends on {@link #protocol} and
 * the provider preset ({@link #providerKey}). Nulls are expected (e.g. SAML rows have no OAuth endpoints).
 */
@Entity
@Table(name = "identity_provider", uniqueConstraints = {
        @UniqueConstraint(name = "uq_idp_tenant_alias", columnNames = {"tenant_id", "alias"})
})
public class IdentityProvider {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64)
    private String tenantId;

    /** URL-safe handle unique within the tenant, e.g. {@code google} or {@code corp-okta}. */
    @Column(nullable = false, length = 64)
    private String alias;

    /** Preset key: GOOGLE, ENTRA, APPLE, GITHUB, OIDC, SAML. */
    @Column(name = "provider_key", nullable = false, length = 32)
    private String providerKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private ProviderProtocol protocol;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled = true;

    // --- OAuth2/OIDC credentials + endpoints ---
    @Column(name = "client_id", length = 320)
    private String clientId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "client_secret", length = 2048)
    private String clientSecret;

    /** OIDC discovery base (Google/Entra/Apple/generic OIDC). */
    @Column(name = "issuer_uri", length = 512)
    private String issuerUri;

    @Column(name = "authorization_uri", length = 512)
    private String authorizationUri;

    @Column(name = "token_uri", length = 512)
    private String tokenUri;

    @Column(name = "user_info_uri", length = 512)
    private String userInfoUri;

    @Column(name = "jwk_set_uri", length = 512)
    private String jwkSetUri;

    @Column(length = 512)
    private String scopes;

    /** Claim/attribute that identifies the user (e.g. {@code email}, {@code sub}, {@code login}). */
    @Column(name = "user_name_attribute", length = 64)
    private String userNameAttribute;

    /** Entra directory (tenant) id, used to template the Entra issuer/endpoints. */
    @Column(name = "provider_directory", length = 128)
    private String providerDirectory;

    // --- SAML ---
    @Column(name = "saml_metadata_url", length = 512)
    private String samlMetadataUrl;

    @Column(name = "saml_entity_id", length = 512)
    private String samlEntityId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected IdentityProvider() {
    }

    public IdentityProvider(UUID id, String tenantId, String alias, String providerKey,
                            ProviderProtocol protocol, String displayName) {
        this.id = id;
        this.tenantId = tenantId;
        this.alias = alias;
        this.providerKey = providerKey;
        this.protocol = protocol;
        this.displayName = displayName;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getAlias() {
        return alias;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public ProviderProtocol getProtocol() {
        return protocol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getAuthorizationUri() {
        return authorizationUri;
    }

    public void setAuthorizationUri(String authorizationUri) {
        this.authorizationUri = authorizationUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public String getUserInfoUri() {
        return userInfoUri;
    }

    public void setUserInfoUri(String userInfoUri) {
        this.userInfoUri = userInfoUri;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    public void setUserNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
    }

    public String getProviderDirectory() {
        return providerDirectory;
    }

    public void setProviderDirectory(String providerDirectory) {
        this.providerDirectory = providerDirectory;
    }

    public String getSamlMetadataUrl() {
        return samlMetadataUrl;
    }

    public void setSamlMetadataUrl(String samlMetadataUrl) {
        this.samlMetadataUrl = samlMetadataUrl;
    }

    public String getSamlEntityId() {
        return samlEntityId;
    }

    public void setSamlEntityId(String samlEntityId) {
        this.samlEntityId = samlEntityId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
