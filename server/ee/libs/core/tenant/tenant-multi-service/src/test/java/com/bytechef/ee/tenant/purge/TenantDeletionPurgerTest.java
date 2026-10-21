/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.purge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.tenant.repository.TenantRepository;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantDeletion;
import com.bytechef.tenant.service.TenantDeletionService;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.tenant.teardown.TenantTeardownContributor;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressWarnings("unchecked")
class TenantDeletionPurgerTest {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

    private final TenantDeletionService tenantDeletionService = mock(TenantDeletionService.class);
    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final TenantService tenantService = mock(TenantService.class);
    private final TenantTeardownContributor contributor = mock(TenantTeardownContributor.class);

    @BeforeEach
    void beforeEach() {
        when(tenantDeletionService.claimForPurge(anyString(), any())).thenReturn(true);
        when(tenantRepository.schemaExists(anyString())).thenReturn(true);
    }

    @Test
    void testPurgeRunsBeforeDropThenDropThenAfterDrop() {
        when(tenantDeletionService.getDueForPurge(NOW)).thenReturn(List.of(deletion("000001")));

        purger(contributor).purgeDueTenants();

        InOrder inOrder = inOrder(contributor, tenantService, tenantDeletionService);

        inOrder.verify(contributor)
            .beforeSchemaDrop("000001");
        inOrder.verify(tenantService)
            .deleteTenant("000001");
        inOrder.verify(contributor)
            .afterSchemaDrop("000001");
        inOrder.verify(tenantDeletionService)
            .markPurged("000001");
    }

    @Test
    void testPurgeRerunsDeletionRequestedBeforeBeforeDrop() {
        when(tenantDeletionService.getDueForPurge(NOW)).thenReturn(List.of(deletion("000001")));

        purger(contributor).purgeDueTenants();

        InOrder inOrder = inOrder(contributor, tenantService);

        inOrder.verify(contributor)
            .onDeletionRequested("000001");
        inOrder.verify(contributor)
            .beforeSchemaDrop("000001");
        inOrder.verify(tenantService)
            .deleteTenant("000001");
    }

    @Test
    void testDeletionRequestedRunsInTenantContext() {
        List<String> tenantIds = new ArrayList<>();

        TenantTeardownContributor recordingContributor = new TenantTeardownContributor() {

            @Override
            public List<String> onDeletionRequested(String tenantId) {
                tenantIds.add(TenantContext.getCurrentTenantId());

                return List.of("webhook still registered");
            }
        };

        when(tenantDeletionService.getDueForPurge(NOW)).thenReturn(List.of(deletion("000001")));

        purger(recordingContributor).purgeDueTenants();

        assertThat(tenantIds).containsExactly("000001");
        verify(tenantService).deleteTenant("000001");
        verify(tenantDeletionService).markPurged("000001");
    }

    @Test
    void testDeletionRequestedFailureDoesNotStopDrop() {
        TenantTeardownContributor failingContributor = mock(TenantTeardownContributor.class);

        when(failingContributor.onDeletionRequested("000001")).thenThrow(new IllegalStateException("boom"));
        when(tenantDeletionService.getDueForPurge(NOW)).thenReturn(List.of(deletion("000001")));

        purger(failingContributor, contributor).purgeDueTenants();

        verify(contributor).onDeletionRequested("000001");
        verify(failingContributor).beforeSchemaDrop("000001");
        verify(tenantService).deleteTenant("000001");
        verify(tenantDeletionService).markPurged("000001");
    }

    @Test
    void testBeforeDropRunsInTenantContextAndAfterDropInDefaultContext() {
        List<String> tenantIds = new ArrayList<>();

        TenantTeardownContributor recordingContributor = new TenantTeardownContributor() {

            @Override
            public void beforeSchemaDrop(String tenantId) {
                tenantIds.add(TenantContext.getCurrentTenantId());
            }

            @Override
            public void afterSchemaDrop(String tenantId) {
                tenantIds.add(TenantContext.getCurrentTenantId());
            }
        };

        when(tenantDeletionService.getDueForPurge(NOW)).thenReturn(List.of(deletion("000001")));

        purger(recordingContributor).purgeDueTenants();

        assertThat(tenantIds).containsExactly("000001", TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testSkipsUnclaimedTenant() {
        when(tenantDeletionService.getDueForPurge(NOW)).thenReturn(List.of(deletion("000001")));
        when(tenantDeletionService.claimForPurge("000001", NOW)).thenReturn(false);

        purger(contributor).purgeDueTenants();

        verify(contributor, never()).beforeSchemaDrop(anyString());
        verify(tenantService, never()).deleteTenant(anyString());
        verify(contributor, never()).afterSchemaDrop(anyString());
        verify(tenantDeletionService, never()).markPurged(anyString());
    }

    @Test
    void testResumesAfterSchemaAlreadyDropped() {
        when(tenantDeletionService.getDueForPurge(NOW)).thenReturn(List.of(deletion("000001")));
        when(tenantRepository.schemaExists("000001")).thenReturn(false);

        purger(contributor).purgeDueTenants();

        verify(contributor, never()).onDeletionRequested(anyString());
        verify(contributor, never()).beforeSchemaDrop(anyString());
        verify(tenantService, never()).deleteTenant(anyString());
        verify(contributor).afterSchemaDrop("000001");
        verify(tenantDeletionService).markPurged("000001");
    }

    @Test
    void testAfterDropFailureLeavesRowPendingForNextSweep() {
        TenantTeardownContributor failingContributor = mock(TenantTeardownContributor.class);
        TenantTeardownContributor followingContributor = mock(TenantTeardownContributor.class);

        doThrow(new IllegalStateException("boom")).when(failingContributor)
            .afterSchemaDrop("000001");

        when(tenantDeletionService.getDueForPurge(NOW))
            .thenReturn(List.of(deletion("000001"), deletion("000002")));

        purger(failingContributor, followingContributor).purgeDueTenants();

        verify(followingContributor).afterSchemaDrop("000001");
        verify(tenantDeletionService, never()).markPurged("000001");
        verify(tenantService).deleteTenant("000002");
        verify(tenantDeletionService).markPurged("000002");
    }

    @Test
    void testBeforeDropFailureSkipsDropAndContinuesWithNextTenant() {
        TenantTeardownContributor failingContributor = mock(TenantTeardownContributor.class);

        doThrow(new IllegalStateException("boom")).when(failingContributor)
            .beforeSchemaDrop("000001");

        when(tenantDeletionService.getDueForPurge(NOW))
            .thenReturn(List.of(deletion("000001"), deletion("000002")));

        purger(failingContributor).purgeDueTenants();

        verify(tenantService, never()).deleteTenant("000001");
        verify(failingContributor, never()).afterSchemaDrop("000001");
        verify(tenantDeletionService, never()).markPurged("000001");
        verify(tenantService).deleteTenant("000002");
        verify(tenantDeletionService).markPurged("000002");
    }

    @Test
    void testClaimFailureContinuesWithNextTenant() {
        when(tenantDeletionService.getDueForPurge(NOW))
            .thenReturn(List.of(deletion("000001"), deletion("000002")));
        when(tenantDeletionService.claimForPurge("000001", NOW)).thenThrow(new IllegalStateException("boom"));

        purger(contributor).purgeDueTenants();

        verify(tenantDeletionService).markPurged("000002");
        verify(tenantDeletionService, never()).markPurged("000001");
    }

    @Test
    void testPurgerIsNotTransactional() throws NoSuchMethodException {
        Method method = TenantDeletionPurger.class.getDeclaredMethod("purgeDueTenants");

        assertThat(TenantDeletionPurger.class.isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(method.isAnnotationPresent(Transactional.class)).isFalse();
    }

    private TenantDeletionPurger purger(TenantTeardownContributor... contributors) {
        ObjectProvider<Clock> clockProvider = mock(ObjectProvider.class);

        when(clockProvider.getIfAvailable(any())).thenReturn(Clock.fixed(NOW, ZoneOffset.UTC));

        return new TenantDeletionPurger(
            tenantDeletionService, tenantRepository, tenantService, List.of(contributors), clockProvider);
    }

    private static TenantDeletion deletion(String tenantId) {
        return new TenantDeletion(
            tenantId, TenantDeletion.Status.PENDING_DELETION, "admin@x", NOW.minusSeconds(100), NOW.minusSeconds(1),
            null, null, null, List.of());
    }
}
