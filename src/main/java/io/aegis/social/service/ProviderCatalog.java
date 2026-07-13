package io.aegis.social.service;

import io.aegis.social.domain.ProviderProtocol;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The catalog of supported identity providers and their presets. Social providers (Google, Microsoft
 * Entra ID, Apple, GitHub) ship with their endpoints/scopes baked in so a tenant only supplies client
 * id/secret (plus a directory id for Entra); generic OIDC and SAML require the admin to supply the
 * issuer/metadata.
 */
public final class ProviderCatalog {

    private static final Map<String, ProviderPreset> PRESETS = new LinkedHashMap<>();

    static {
        add(new ProviderPreset("GOOGLE", "Google", ProviderProtocol.OIDC,
                "https://accounts.google.com", null, null, null, null,
                "openid email profile", "email", List.of(), null));

        add(new ProviderPreset("ENTRA", "Microsoft Entra ID", ProviderProtocol.OIDC,
                "https://login.microsoftonline.com/{directory}/v2.0", null, null, null, null,
                "openid email profile", "email", List.of("providerDirectory"),
                "providerDirectory is your Entra directory (tenant) id, or 'common' for multi-tenant."));

        add(new ProviderPreset("APPLE", "Apple", ProviderProtocol.OIDC,
                "https://appleid.apple.com", null, null, null, null,
                "openid email name", "email", List.of(),
                "Apple returns claims via form_post and requires a JWT client secret (handled at runtime)."));

        add(new ProviderPreset("GITHUB", "GitHub", ProviderProtocol.OAUTH2,
                null,
                "https://github.com/login/oauth/authorize",
                "https://github.com/login/oauth/access_token",
                "https://api.github.com/user", null,
                "read:user user:email", "login", List.of(),
                "GitHub is OAuth2 (no id_token); the user is read from the /user API."));

        add(new ProviderPreset("OIDC", "Generic OIDC", ProviderProtocol.OIDC,
                null, null, null, null, null,
                "openid email profile", "email", List.of("issuerUri"),
                "Any OpenID Connect provider with a discovery document."));

        add(new ProviderPreset("SAML", "SAML 2.0", ProviderProtocol.SAML,
                null, null, null, null, null,
                null, null, List.of("samlMetadataUrl"),
                "Corporate SAML 2.0 IdP; provide the IdP metadata URL."));
    }

    private ProviderCatalog() {
    }

    private static void add(ProviderPreset preset) {
        PRESETS.put(preset.key(), preset);
    }

    public static List<ProviderPreset> all() {
        return List.copyOf(PRESETS.values());
    }

    public static ProviderPreset require(String key) {
        ProviderPreset preset = key == null ? null : PRESETS.get(key.toUpperCase());
        if (preset == null) {
            throw new IllegalArgumentException("unknown provider: " + key
                    + ". Supported: " + PRESETS.keySet());
        }
        return preset;
    }
}
