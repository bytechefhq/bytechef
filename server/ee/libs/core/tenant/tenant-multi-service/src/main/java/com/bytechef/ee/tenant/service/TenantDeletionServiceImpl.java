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
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(TenantDeletionServiceImpl.class);

    private static final long REFRESH_INTERVAL_MILLIS = 30_000L;

    private final ReentrantLock refreshLock = new ReentrantLock();
    private final TenantDeletionRepository tenantDeletionRepository;

    private volatile Snapshot snapshot = new Snapshot(Set.of(), 0L, false);

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
        Snapshot currentSnapshot = snapshot;

        if (isStale(currentSnapshot)) {
            refreshLock.lock();

            try {
                if (isStale(snapshot)) {
                    reload(null);
                }

                currentSnapshot = snapshot;
            } finally {
                refreshLock.unlock();
            }
        }

        return currentSnapshot.inactiveTenantIds()
            .contains(tenantId);
    }

    @Override
    public boolean markPendingDeletion(String tenantId, String requestedBy, Instant purgeAfter) {
        boolean marked = tenantDeletionRepository.markPendingDeletion(tenantId, requestedBy, purgeAfter);

        if (marked) {
            reloadLocked(tenantId);
        }

        return marked;
    }

    @Override
    public void markPurged(String tenantId) {
        tenantDeletionRepository.markPurged(tenantId);

        reloadLocked(tenantId);
    }

    @Override
    public void recordStripeIds(String tenantId, String customerId, String subscriptionId) {
        tenantDeletionRepository.recordStripeIds(tenantId, customerId, subscriptionId);
    }

    @Override
    public void recordTeardownFailures(String tenantId, List<String> failures) {
        tenantDeletionRepository.recordTeardownFailures(tenantId, failures);
    }

    private boolean isStale(Snapshot currentSnapshot) {
        return !currentSnapshot.loaded() ||
            System.currentTimeMillis() - currentSnapshot.loadedAtMillis() > REFRESH_INTERVAL_MILLIS;
    }

    private void reloadLocked(String locallyInactiveTenantId) {
        refreshLock.lock();

        try {
            reload(locallyInactiveTenantId);
        } finally {
            refreshLock.unlock();
        }
    }

    private void reload(String locallyInactiveTenantId) {
        Set<String> tenantIds = new HashSet<>();
        Snapshot previousSnapshot = snapshot;
        long loadedAtMillis = previousSnapshot.loadedAtMillis();
        boolean loaded = previousSnapshot.loaded();

        try {
            tenantIds.addAll(tenantDeletionRepository.findInactiveTenantIds());

            loadedAtMillis = System.currentTimeMillis();
            loaded = true;
        } catch (RuntimeException runtimeException) {
            log.warn("Failed to refresh the inactive tenant ids, keeping the last known set", runtimeException);

            tenantIds.addAll(previousSnapshot.inactiveTenantIds());
        }

        if (locallyInactiveTenantId != null) {
            tenantIds.add(locallyInactiveTenantId);
        }

        snapshot = new Snapshot(Set.copyOf(tenantIds), loadedAtMillis, loaded);
    }

    private record Snapshot(Set<String> inactiveTenantIds, long loadedAtMillis, boolean loaded) {
    }
}
