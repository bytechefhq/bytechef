/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.tenant.repository.TenantDeletionRepository;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class TenantDeletionServiceTest {

    @Test
    void testIsInactiveSeesLocalMarkImmediately() {
        TenantDeletionRepository tenantDeletionRepository = mock(TenantDeletionRepository.class);
        Instant purgeAfter = Instant.now();

        when(tenantDeletionRepository.findInactiveTenantIds()).thenReturn(Set.of());
        when(tenantDeletionRepository.markPendingDeletion("000001", "admin@x", purgeAfter)).thenReturn(true);

        TenantDeletionServiceImpl tenantDeletionService = new TenantDeletionServiceImpl(tenantDeletionRepository);

        assertThat(tenantDeletionService.isInactive("000001")).isFalse();
        assertThat(tenantDeletionService.markPendingDeletion("000001", "admin@x", purgeAfter)).isTrue();
        assertThat(tenantDeletionService.isInactive("000001")).isTrue();
    }

    @Test
    void testIsInactiveSeesLocalPurgeImmediately() {
        TenantDeletionRepository tenantDeletionRepository = mock(TenantDeletionRepository.class);

        when(tenantDeletionRepository.findInactiveTenantIds()).thenReturn(Set.of());

        TenantDeletionServiceImpl tenantDeletionService = new TenantDeletionServiceImpl(tenantDeletionRepository);

        tenantDeletionService.markPurged("000001");

        assertThat(tenantDeletionService.isInactive("000001")).isTrue();
    }

    @Test
    void testIsInactiveIsFalseForUnknownTenant() {
        TenantDeletionRepository tenantDeletionRepository = mock(TenantDeletionRepository.class);

        when(tenantDeletionRepository.findInactiveTenantIds()).thenReturn(Set.of("000002"));

        TenantDeletionServiceImpl tenantDeletionService = new TenantDeletionServiceImpl(tenantDeletionRepository);

        assertThat(tenantDeletionService.isInactive("000001")).isFalse();
        assertThat(tenantDeletionService.isInactive("000002")).isTrue();
    }

    @Test
    void testIsInactiveIsFalseForNullTenant() {
        TenantDeletionRepository tenantDeletionRepository = mock(TenantDeletionRepository.class);

        when(tenantDeletionRepository.findInactiveTenantIds()).thenReturn(Set.of("000002"));

        TenantDeletionServiceImpl tenantDeletionService = new TenantDeletionServiceImpl(tenantDeletionRepository);

        assertThat(tenantDeletionService.isInactive(null)).isFalse();
    }

    @Test
    void testRefreshFailureKeepsLastGoodSet() {
        TenantDeletionRepository tenantDeletionRepository = mock(TenantDeletionRepository.class);

        when(tenantDeletionRepository.findInactiveTenantIds())
            .thenReturn(Set.of("000002"))
            .thenThrow(new RuntimeException("database down"))
            .thenReturn(Set.of("000002", "000003"));

        TenantDeletionServiceImpl tenantDeletionService = new TenantDeletionServiceImpl(tenantDeletionRepository);

        assertThat(tenantDeletionService.isInactive("000002")).isTrue();

        tenantDeletionService.markPurged("000009");

        assertThat(tenantDeletionService.isInactive("000002")).isTrue();
        assertThat(tenantDeletionService.isInactive("000009")).isTrue();
        assertThat(tenantDeletionService.isInactive("000003")).isFalse();
    }

    @Test
    void testStaleRefreshDoesNotHideConcurrentLocalMark() throws Exception {
        TenantDeletionRepository tenantDeletionRepository = mock(TenantDeletionRepository.class);
        CountDownLatch firstReadEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstRead = new CountDownLatch(1);
        AtomicInteger readCount = new AtomicInteger();

        when(tenantDeletionRepository.findInactiveTenantIds()).thenAnswer(invocation -> {
            if (readCount.incrementAndGet() == 1) {
                firstReadEntered.countDown();

                if (!releaseFirstRead.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("First read was never released");
                }
            }

            return Set.of();
        });
        when(tenantDeletionRepository.markPendingDeletion("000001", "admin@x", Instant.EPOCH)).thenReturn(true);

        TenantDeletionServiceImpl tenantDeletionService = new TenantDeletionServiceImpl(tenantDeletionRepository);

        Thread staleReader = new Thread(() -> tenantDeletionService.isInactive("000009"));

        staleReader.start();

        assertThat(firstReadEntered.await(10, TimeUnit.SECONDS)).isTrue();

        Thread marker = new Thread(() -> tenantDeletionService.markPendingDeletion("000001", "admin@x", Instant.EPOCH));

        marker.start();
        marker.join(500);

        releaseFirstRead.countDown();

        staleReader.join(10_000);
        marker.join(10_000);

        assertThat(tenantDeletionService.isInactive("000001")).isTrue();
    }
}
