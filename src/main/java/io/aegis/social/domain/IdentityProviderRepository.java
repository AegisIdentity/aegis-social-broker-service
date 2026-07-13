package io.aegis.social.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Tenant-scoped persistence for {@link IdentityProvider}. No bare findById is exposed to callers. */
public interface IdentityProviderRepository extends JpaRepository<IdentityProvider, UUID> {

    List<IdentityProvider> findByTenantIdOrderByDisplayName(String tenantId);

    List<IdentityProvider> findByTenantIdAndEnabledTrueOrderByDisplayName(String tenantId);

    Optional<IdentityProvider> findByTenantIdAndId(String tenantId, UUID id);

    Optional<IdentityProvider> findByTenantIdAndAlias(String tenantId, String alias);

    boolean existsByTenantIdAndAlias(String tenantId, String alias);
}
