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
}
