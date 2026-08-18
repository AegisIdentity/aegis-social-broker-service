package io.aegis.social.service;

import io.aegis.social.domain.IdentityProvider;
import io.aegis.social.domain.IdentityProviderRepository;
import io.aegis.social.domain.ProviderProtocol;
import io.aegis.social.service.BrokerExceptions.DuplicateProviderException;
import io.aegis.social.service.BrokerExceptions.ProviderNotFoundException;
import io.aegis.social.service.BrokerExceptions.ProviderValidationException;
import io.aegis.social.web.IdentityProviderDtos.CreateProviderRequest;
import io.aegis.social.web.IdentityProviderDtos.ProviderView;
import io.aegis.social.web.IdentityProviderDtos.UpdateProviderRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Manages per-tenant identity providers. Every operation is scoped to a tenant (taken from the caller's
 * token, never the body). Presets fill in a provider's endpoints/scopes so a tenant only supplies its
 * client id/secret (plus a directory id for Entra, an issuer for generic OIDC, or metadata for SAML).
 * The client secret is stored but never returned.
 */
@Service
public class IdentityProviderService {

    private final IdentityProviderRepository providers;
    private final io.aegis.commons.audit.AuditRecorder audit;

    public IdentityProviderService(IdentityProviderRepository providers,
                                   io.aegis.commons.audit.AuditRecorder audit) {
        this.providers = providers;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ProviderView> list(String tenantId) {
        requireTenant(tenantId);
        return providers.findByTenantIdOrderByDisplayName(tenantId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public ProviderView get(String tenantId, UUID id) {
        return toView(load(tenantId, id));
    }

    @Transactional
    public ProviderView create(String tenantId, CreateProviderRequest req) {
        requireTenant(tenantId);
        ProviderPreset preset = catalogEntry(req.providerKey());
        if (providers.existsByTenantIdAndAlias(tenantId, req.alias())) {
            throw new DuplicateProviderException("a provider with alias '" + req.alias() + "' already exists");
        }
        validateRequiredFields(preset, req);

        String displayName = StringUtils.hasText(req.displayName()) ? req.displayName() : preset.displayName();
        IdentityProvider p = new IdentityProvider(
                UUID.randomUUID(), tenantId, req.alias(), preset.key(), preset.protocol(), displayName);

        applyPreset(p, preset, req);
        if (preset.protocol() != ProviderProtocol.SAML) {
            p.setClientId(req.clientId());
            p.setClientSecret(req.clientSecret());
        }
        p.setScopes(StringUtils.hasText(req.scopes()) ? req.scopes() : preset.defaultScopes());
        p.setUserNameAttribute(StringUtils.hasText(req.userNameAttribute())
                ? req.userNameAttribute() : preset.userNameAttribute());
        IdentityProvider saved = providers.save(p);
        audit.record("idp", "idp.created", tenantId, "system", saved.getAlias(),
                "provider", saved.getProviderKey());
        return toView(saved);
    }

    @Transactional
    public ProviderView update(String tenantId, UUID id, UpdateProviderRequest req) {
        IdentityProvider p = load(tenantId, id);
        if (req.enabled() != null) {
            p.setEnabled(req.enabled());
        }
        if (StringUtils.hasText(req.displayName())) {
            p.setDisplayName(req.displayName());
        }
        if (req.scopes() != null) {
            p.setScopes(req.scopes());
        }
        if (StringUtils.hasText(req.clientSecret())) {
            p.setClientSecret(req.clientSecret());
        }
        p.touch();
        IdentityProvider saved = providers.save(p);
        audit.record("idp", "idp.updated", tenantId, "system", saved.getAlias());
        return toView(saved);
    }

    @Transactional
    public void delete(String tenantId, UUID id) {
        IdentityProvider p = load(tenantId, id);
        providers.delete(p);
        audit.record("idp", "idp.deleted", tenantId, "system", p.getAlias());
    }

    // --- internals ---

    private IdentityProvider load(String tenantId, UUID id) {
        requireTenant(tenantId);
        return providers.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ProviderNotFoundException("no such identity provider in tenant"));
    }

    private static ProviderPreset catalogEntry(String providerKey) {
        try {
            return ProviderCatalog.require(providerKey);
        } catch (IllegalArgumentException ex) {
            throw new ProviderValidationException(ex.getMessage());
        }
    }

    private static void validateRequiredFields(ProviderPreset preset, CreateProviderRequest req) {
        if (preset.protocol() != ProviderProtocol.SAML) {
            if (!StringUtils.hasText(req.clientId()) || !StringUtils.hasText(req.clientSecret())) {
                throw new ProviderValidationException("clientId and clientSecret are required for " + preset.key());
            }
        }
        for (String field : preset.requiredFields()) {
            boolean present = switch (field) {
                case "issuerUri" -> StringUtils.hasText(req.issuerUri());
                case "providerDirectory" -> StringUtils.hasText(req.providerDirectory());
                case "samlMetadataUrl" -> StringUtils.hasText(req.samlMetadataUrl());
                default -> true;
            };
            if (!present) {
                throw new ProviderValidationException(field + " is required for " + preset.key());
            }
        }
    }

    private static void applyPreset(IdentityProvider p, ProviderPreset preset, CreateProviderRequest req) {
        switch (preset.protocol()) {
            case OIDC -> {
                String issuer = preset.issuerUri();
                if (issuer == null) {
                    issuer = req.issuerUri(); // generic OIDC
                } else if (issuer.contains("{directory}")) {
                    p.setProviderDirectory(req.providerDirectory());
                    issuer = issuer.replace("{directory}", req.providerDirectory()); // Entra
                }
                p.setIssuerUri(issuer);
            }
            case OAUTH2 -> {
                p.setAuthorizationUri(preset.authorizationUri());
                p.setTokenUri(preset.tokenUri());
                p.setUserInfoUri(preset.userInfoUri());
            }
            case SAML -> {
                p.setSamlMetadataUrl(req.samlMetadataUrl());
                p.setSamlEntityId(req.samlEntityId());
            }
        }
    }

    private ProviderView toView(IdentityProvider p) {
        return new ProviderView(
                p.getId().toString(), p.getAlias(), p.getProviderKey(), p.getProtocol(),
                p.getDisplayName(), p.isEnabled(), p.getClientId(), p.getScopes(),
                p.getIssuerUri(), p.getSamlMetadataUrl(), StringUtils.hasText(p.getClientSecret()));
    }

    private static void requireTenant(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalArgumentException("tenantId is required");
        }
    }
}
