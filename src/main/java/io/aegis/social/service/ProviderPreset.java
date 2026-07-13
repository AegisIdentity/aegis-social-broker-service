package io.aegis.social.service;

import io.aegis.social.domain.ProviderProtocol;
import java.util.List;

/**
 * A provider preset: the fixed defaults for a well-known provider (endpoints, scopes, protocol) plus
 * the list of extra inputs the admin must supply beyond client id/secret. The console renders its
 * "add provider" form from these; the service applies them when creating a provider.
 *
 * <p>{@code issuerUri} may contain a {@code {directory}} placeholder (Entra), substituted from the
 * admin-supplied {@code providerDirectory}.
 */
public record ProviderPreset(
        String key,
        String displayName,
        ProviderProtocol protocol,
        String issuerUri,
        String authorizationUri,
        String tokenUri,
        String userInfoUri,
        String jwkSetUri,
        String defaultScopes,
        String userNameAttribute,
        List<String> requiredFields,
        String notes) {
}
