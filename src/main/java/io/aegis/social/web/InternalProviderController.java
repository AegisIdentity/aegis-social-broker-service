package io.aegis.social.web;

import io.aegis.social.domain.IdentityProvider;
import io.aegis.social.domain.IdentityProviderRepository;
import io.aegis.social.domain.ProviderProtocol;
import io.aegis.social.service.BrokerExceptions.ProviderNotFoundException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service endpoints for the authorization-server to build federated client registrations
 * and render the login page. Under {@code /internal/**}, gated by {@code SCOPE_idp:resolve} (only the
 * AS's own service token carries it). Unlike the admin API, {@code resolve} returns the client secret,
 * because the AS needs it to exchange the authorization code with the upstream provider.
 *
 * <p><strong>M-svc-2 / deployment requirement:</strong> {@code resolve} returns a decrypted upstream
 * client secret in its response body. This endpoint is in-network only (scope {@code idp:resolve}, not
 * granted to any tenant/user token) and MUST be reached over TLS/mTLS on the service mesh — never over
 * plaintext HTTP — so the secret is not exposed in transit. The secret is encrypted at rest
 * (AES-256-GCM); this is the one place it is legitimately decrypted for the AS's RP flow.
 */
@RestController
public class InternalProviderController {

    private final IdentityProviderRepository providers;

    public InternalProviderController(IdentityProviderRepository providers) {
        this.providers = providers;
    }

    /** Enabled providers for a tenant, minimal fields — used to render the login page's SSO buttons. */
    @GetMapping("/internal/identity-providers")
    public List<InternalDtos.ProviderSummary> listEnabled(@RequestParam String tenant) {
        return providers.findByTenantIdAndEnabledTrueOrderByDisplayName(tenant).stream()
                .map(p -> new InternalDtos.ProviderSummary(
                        p.getAlias(), p.getProviderKey(), p.getProtocol(), p.getDisplayName()))
                .toList();
    }

    /** Full configuration (including the client secret) for one provider — for the AS's RP flow. */
    @GetMapping("/internal/identity-providers/resolve")
    public InternalDtos.ProviderConfig resolve(@RequestParam String tenant, @RequestParam String alias) {
        IdentityProvider p = providers.findByTenantIdAndAlias(tenant, alias)
                .filter(IdentityProvider::isEnabled)
                .orElseThrow(() -> new ProviderNotFoundException(
                        "no enabled provider '" + alias + "' for tenant"));
        return new InternalDtos.ProviderConfig(
                p.getAlias(), p.getProviderKey(), p.getProtocol(), p.getDisplayName(),
                p.getClientId(), p.getClientSecret(), p.getIssuerUri(), p.getAuthorizationUri(),
                p.getTokenUri(), p.getUserInfoUri(), p.getJwkSetUri(), p.getScopes(),
                p.getUserNameAttribute(), p.getSamlMetadataUrl(), p.getSamlEntityId());
    }

    /** Internal DTOs. {@code ProviderConfig} deliberately includes the secret (service-only). */
    public static final class InternalDtos {
        private InternalDtos() {
        }

        public record ProviderSummary(String alias, String providerKey, ProviderProtocol protocol,
                                      String displayName) {
        }

        public record ProviderConfig(String alias, String providerKey, ProviderProtocol protocol,
                                     String displayName, String clientId, String clientSecret,
                                     String issuerUri, String authorizationUri, String tokenUri,
                                     String userInfoUri, String jwkSetUri, String scopes,
                                     String userNameAttribute, String samlMetadataUrl,
                                     String samlEntityId) {
        }
    }
}
