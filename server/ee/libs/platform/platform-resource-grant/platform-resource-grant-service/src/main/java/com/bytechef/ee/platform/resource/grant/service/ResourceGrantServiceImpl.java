/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.resource.grant.service;

import com.bytechef.ee.platform.resource.grant.domain.ResourceGrant;
import com.bytechef.ee.platform.resource.grant.repository.ResourceGrantRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.PostgresDialect;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ResourceGrantServiceImpl implements ResourceGrantService {

    private final boolean onConflictSupported;
    private final ResourceGrantRepository resourceGrantRepository;
    private final ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry;

    @SuppressFBWarnings("EI")
    public ResourceGrantServiceImpl(
        Dialect dialect, ResourceGrantRepository resourceGrantRepository,
        ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry) {

        this.onConflictSupported = dialect instanceof PostgresDialect;
        this.resourceGrantRepository = resourceGrantRepository;
        this.resourceVisibilityPolicyRegistry = resourceVisibilityPolicyRegistry;
    }

    @Override
    public void grant(String resourceType, long resourceId, long userId) {
        if (!resourceVisibilityPolicyRegistry.supports(resourceType, ResourceVisibility.PRIVATE)) {
            throw new IllegalArgumentException(
                "Resource type '%s' cannot be withheld, so it cannot be granted".formatted(resourceType));
        }

        // Idempotency is the database's job here (ON CONFLICT DO NOTHING), not an exception handler's. Catching
        // DuplicateKeyException would not work: PostgreSQL aborts the transaction on a constraint violation, so the
        // commit fails with UnexpectedRollbackException even though the exception was handled.
        String createdBy = SecurityUtils.fetchCurrentUserLogin()
            .orElse(SecurityUtils.SYSTEM_LOGIN);

        if (onConflictSupported) {
            resourceGrantRepository.insertIfAbsent(resourceType, resourceId, userId, createdBy, Instant.now());
        } else {
            resourceGrantRepository.insertIfAbsentWithoutOnConflict(
                resourceType, resourceId, userId, createdBy, Instant.now());
        }
    }

    @Override
    public void revoke(String resourceType, long resourceId, long userId) {
        resourceGrantRepository.findByResourceTypeAndResourceIdAndUserId(resourceType, resourceId, userId)
            .ifPresent(resourceGrantRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getGrantedUserIds(String resourceType, long resourceId) {
        List<ResourceGrant> resourceGrants = resourceGrantRepository.findAllByResourceTypeAndResourceId(
            resourceType, resourceId);

        return resourceGrants.stream()
            .map(ResourceGrant::getUserId)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> filterGrantedResourceIds(String resourceType, long userId, Collection<Long> resourceIds) {
        if (resourceIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(resourceGrantRepository.findGrantedResourceIds(resourceType, userId, resourceIds));
    }

    @Override
    public void deleteGrants(String resourceType, long resourceId) {
        resourceGrantRepository.deleteAllByResourceTypeAndResourceId(resourceType, resourceId);
    }

    @Override
    public void revokeUserGrants(String resourceType, long userId, Collection<Long> resourceIds) {
        if (resourceIds.isEmpty()) {
            return;
        }

        resourceGrantRepository.deleteUserGrants(resourceType, userId, resourceIds);
    }
}
