package io.aegis.social.web;

import io.aegis.social.domain.ProviderProtocol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/** Payloads for the identity-provider admin API. The client secret is never returned. */
public final class IdentityProviderDtos {

    private IdentityProviderDtos() {
    }

    /** A registered provider as seen by the admin. {@code hasSecret} stands in for the never-returned secret. */
    public record ProviderView(
            String id,
            String alias,
            String providerKey,
            ProviderProtocol protocol,
            String displayName,
            boolean enabled,
            String clientId,
            String scopes,
            String issuerUri,
            String samlMetadataUrl,
            boolean hasSecret) {
    }

    /**
     * Registers a provider. {@code providerKey} selects the preset (GOOGLE/ENTRA/APPLE/GITHUB/OIDC/SAML);
     * per-preset required fields (issuerUri for generic OIDC, providerDirectory for Entra, samlMetadataUrl
     * for SAML, clientId/clientSecret for all OAuth/OIDC) are validated server-side.
     */
    public record CreateProviderRequest(
            @NotBlank String providerKey,
            @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,62}$",
                    message = "alias must be a lowercase DNS-safe slug") String alias,
            String displayName,
            String clientId,
            String clientSecret,
            String scopes,
            String userNameAttribute,
            String issuerUri,
            String providerDirectory,
            String samlMetadataUrl,
            String samlEntityId) {
    }

    /** Partial update. Any null field is left unchanged; a non-null {@code clientSecret} rotates it. */
    public record UpdateProviderRequest(
            Boolean enabled,
            String displayName,
            String scopes,
            String clientSecret) {
    }

    /** A catalog entry the console renders its "add provider" form from. */
    public record CatalogEntry(
            String key,
            String displayName,
            ProviderProtocol protocol,
            String defaultScopes,
            List<String> requiredFields,
            String notes) {
    }
}
