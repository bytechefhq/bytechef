/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.service;

import com.bytechef.ee.tenant.repository.TenantDeletionRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import com.bytechef.tenant.domain.TenantDeletion;
import com.bytechef.tenant.service.TenantDeletionService;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Keeps a cached set of inactive tenant ids so the per-request {@link #isInactive(String)} check does not query the
 * database. The set is reloaded when older than {@link #REFRESH_INTERVAL_MILLIS} and right after every local
 * transition, so the node that accepted a deletion or purge sees it at once.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnMultiTenant
class TenantDeletionServiceImpl implements TenantDeletionService {

    private static final long REFRESH_INTERVAL_MILLIS = 30_000L;

    private final TenantDeletionRepository tenantDeletionRepository;

    private volatile Set<String> inactiveTenantIds = Set.of();
    private volatile long loadedAtMillis;
    private volatile boolean loaded;

    TenantDeletionServiceImpl(TenantDeletionRepository tenantDeletionRepository) {
        this.tenantDeletionRepository = tenantDeletionRepository;
    }

    @Override
    public boolean claimForPurge(String tenantId, Instant now) {
        return tenantDeletionRepository.claimForPurge(tenantId, now);
    }

    @Override
    public Optional<TenantDeletion> fetchTenantDeletion(String tenantId) {
        return tenantDeletionRepository.fetchTenantDeletion(tenantId);
    }

    @Override
    public List<TenantDeletion> getDueForPurge(Instant now) {
        return tenantDeletionRepository.getDueForPurge(now);
    }

    @Override
    public boolean isInactive(String tenantId) {
        if (!loaded || System.currentTimeMillis() - loadedAtMillis > REFRESH_INTERVAL_MILLIS) {
            reload(null);
        }

        return inactiveTenantIds.contains(tenantId);
    }

    @Override
    public boolean markPendingDeletion(String tenantId, String requestedBy, Instant purgeAfter) {
        boolean marked = tenantDeletionRepository.markPendingDeletion(tenantId, requestedBy, purgeAfter);

        if (marked) {
            reload(tenantId);
        }

        return marked;
    }

    @Override
    public void markPurged(String tenantId) {
        tenantDeletionRepository.markPurged(tenantId);

        reload(tenantId);
    }

    @Override
    public void recordStripeIds(String tenantId, String customerId, String subscriptionId) {
        tenantDeletionRepository.recordStripeIds(tenantId, customerId, subscriptionId);
    }

    @Override
    public void recordTeardownFailures(String tenantId, List<String> failures) {
        tenantDeletionRepository.recordTeardownFailures(tenantId, failures);
    }

    private void reload(String locallyInactiveTenantId) {
        Set<String> tenantIds = new HashSet<>(tenantDeletionRepository.findInactiveTenantIds());

        if (locallyInactiveTenantId != null) {
            tenantIds.add(locallyInactiveTenantId);
        }

        inactiveTenantIds = Set.copyOf(tenantIds);
        loadedAtMillis = System.currentTimeMillis();
        loaded = true;
    }
}
