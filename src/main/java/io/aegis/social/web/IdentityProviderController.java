package io.aegis.social.web;

import io.aegis.social.service.IdentityProviderService;
import io.aegis.social.service.ProviderCatalog;
import io.aegis.social.web.IdentityProviderDtos.CatalogEntry;
import io.aegis.social.web.IdentityProviderDtos.CreateProviderRequest;
import io.aegis.social.web.IdentityProviderDtos.ProviderView;
import io.aegis.social.web.IdentityProviderDtos.UpdateProviderRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Per-tenant identity-provider registry. Secured by {@code SCOPE_idp:admin}; the tenant is taken from
 * the caller's token, so an admin manages only their own organization's providers. Client secrets are
 * write-only (accepted on create/update, never returned).
 */
@RestController
public class IdentityProviderController {

    private final IdentityProviderService service;

    public IdentityProviderController(IdentityProviderService service) {
        this.service = service;
    }

    /** The catalog of supported providers + their presets, for rendering the "add provider" form. */
    @GetMapping("/api/v1/identity-providers/catalog")
    public List<CatalogEntry> catalog() {
        return ProviderCatalog.all().stream()
                .map(p -> new CatalogEntry(p.key(), p.displayName(), p.protocol(),
                        p.defaultScopes(), p.requiredFields(), p.notes()))
                .toList();
    }

    @GetMapping("/api/v1/identity-providers")
    public List<ProviderView> list(@AuthenticationPrincipal Jwt caller) {
        return service.list(tenantOf(caller));
    }

    @GetMapping("/api/v1/identity-providers/{id}")
    public ProviderView get(@PathVariable UUID id, @AuthenticationPrincipal Jwt caller) {
        return service.get(tenantOf(caller), id);
    }

    @PostMapping("/api/v1/identity-providers")
    public ResponseEntity<ProviderView> create(@Valid @RequestBody CreateProviderRequest request,
                                               @AuthenticationPrincipal Jwt caller) {
        ProviderView created = service.create(tenantOf(caller), request);
        return ResponseEntity.created(URI.create("/api/v1/identity-providers/" + created.id())).body(created);
    }

    @PutMapping("/api/v1/identity-providers/{id}")
    public ProviderView update(@PathVariable UUID id, @Valid @RequestBody UpdateProviderRequest request,
                               @AuthenticationPrincipal Jwt caller) {
        return service.update(tenantOf(caller), id, request);
    }

    @DeleteMapping("/api/v1/identity-providers/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt caller) {
        service.delete(tenantOf(caller), id);
        return ResponseEntity.noContent().build();
    }

    private static String tenantOf(Jwt caller) {
        String tenant = caller.getClaimAsString("tenant");
        if (tenant == null || tenant.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "token carries no tenant");
        }
        return tenant;
    }
}
