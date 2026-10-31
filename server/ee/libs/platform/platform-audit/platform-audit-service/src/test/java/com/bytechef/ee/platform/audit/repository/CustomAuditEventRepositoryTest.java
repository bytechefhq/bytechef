/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.AuditEventConverter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomAuditEventRepositoryTest {

    private static final String TENANT_ID = "000001";

    private final PersistenceAuditEventRepository persistenceAuditEventRepository =
        mock(PersistenceAuditEventRepository.class);
    private final List<String> savedTenantIds = new ArrayList<>();
    private final TenantService tenantService = mock(TenantService.class);

    @BeforeEach
    void beforeEach() {
        TenantContext.resetCurrentTenantId();

        when(persistenceAuditEventRepository.save(any(PersistentAuditEvent.class))).thenAnswer(invocation -> {
            savedTenantIds.add(TenantContext.getCurrentTenantId());

            return invocation.getArgument(0);
        });

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testEmailPrincipalIsWrittenInItsTenant() {
        when(tenantService.getTenantIdsByUserEmail("test1@gmail.com")).thenReturn(List.of(TENANT_ID, "000002"));

        CustomAuditEventRepository customAuditEventRepository = createRepository(tenantService);

        customAuditEventRepository.add(createAuditEvent("test1@gmail.com", "AUTHENTICATION_SUCCESS"));

        assertThat(savedTenantIds).containsExactly(TENANT_ID);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
        verify(tenantService, never()).getTenantIdsByUserLogin(anyString());
    }

    @Test
    void testLoginPrincipalIsWrittenInItsTenant() {
        when(tenantService.getTenantIdsByUserLogin("admin")).thenReturn(List.of(TENANT_ID));

        CustomAuditEventRepository customAuditEventRepository = createRepository(tenantService);

        customAuditEventRepository.add(createAuditEvent("Admin", "LOGOUT_SUCCESS"));

        assertThat(savedTenantIds).containsExactly(TENANT_ID);
        verify(tenantService, never()).getTenantIdsByUserEmail(anyString());
    }

    @Test
    void testAlreadySetTenantIsKept() {
        TenantContext.setCurrentTenantId("000002");

        CustomAuditEventRepository customAuditEventRepository = createRepository(tenantService);

        customAuditEventRepository.add(createAuditEvent("test1@gmail.com", "AUTHENTICATION_SUCCESS"));

        assertThat(savedTenantIds).containsExactly("000002");
        verify(tenantService, never()).getTenantIdsByUserEmail(anyString());
        verify(tenantService, never()).getTenantIdsByUserLogin(anyString());
    }

    @Test
    void testUnknownPrincipalIsWrittenInDefaultTenant() {
        when(tenantService.getTenantIdsByUserEmail("unknown@gmail.com")).thenReturn(List.of());
        when(tenantService.getTenantIdsByUserLogin("[credential]")).thenReturn(List.of());

        CustomAuditEventRepository customAuditEventRepository = createRepository(tenantService);

        customAuditEventRepository.add(createAuditEvent("unknown@gmail.com", "AUTHENTICATION_FAILURE"));
        customAuditEventRepository.add(createAuditEvent("[credential]", "AUTHENTICATION_FAILURE"));

        assertThat(savedTenantIds).containsExactly(TenantContext.DEFAULT_TENANT_ID, TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testFailingTenantResolutionStillWritesInDefaultTenant() {
        when(tenantService.getTenantIdsByUserEmail("test1@gmail.com")).thenThrow(new IllegalStateException("down"));

        CustomAuditEventRepository customAuditEventRepository = createRepository(tenantService);

        customAuditEventRepository.add(createAuditEvent("test1@gmail.com", "AUTHENTICATION_SUCCESS"));

        assertThat(savedTenantIds).containsExactly(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testSingleTenantIsWrittenInDefaultTenantWithoutLookup() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        CustomAuditEventRepository singleTenantAuditEventRepository = createRepository(tenantService);

        singleTenantAuditEventRepository.add(createAuditEvent("test1@gmail.com", "AUTHENTICATION_SUCCESS"));

        CustomAuditEventRepository noTenantServiceAuditEventRepository = createRepository(null);

        noTenantServiceAuditEventRepository.add(createAuditEvent("admin", "AUTHENTICATION_SUCCESS"));

        assertThat(savedTenantIds).containsExactly(TenantContext.DEFAULT_TENANT_ID, TenantContext.DEFAULT_TENANT_ID);
        verify(tenantService, never()).getTenantIdsByUserEmail(anyString());
        verify(tenantService, never()).getTenantIdsByUserLogin(anyString());
    }

    @Test
    void testAuthorizationFailureAndAnonymousUserAreSkipped() {
        CustomAuditEventRepository customAuditEventRepository = createRepository(tenantService);

        customAuditEventRepository.add(createAuditEvent("test1@gmail.com", "AUTHORIZATION_FAILURE"));
        customAuditEventRepository.add(
            createAuditEvent(CustomAuditEventRepository.ANONYMOUS_USER, "AUTHENTICATION_SUCCESS"));

        assertThat(savedTenantIds).isEmpty();
        verify(tenantService, never()).getTenantIdsByUserEmail(anyString());
        verify(tenantService, never()).getTenantIdsByUserLogin(anyString());
    }

    @Test
    void testNewTransactionStartsInsideTheResolvedTenant() {
        when(tenantService.getTenantIdsByUserEmail("test1@gmail.com")).thenReturn(List.of(TENANT_ID));

        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        List<String> transactionTenantIds = new ArrayList<>();

        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> {
            transactionTenantIds.add(TenantContext.getCurrentTenantId());

            return mock(TransactionStatus.class);
        });

        CustomAuditEventRepository customAuditEventRepository = createRepository(tenantService, transactionManager);

        customAuditEventRepository.add(createAuditEvent("test1@gmail.com", "AUTHENTICATION_SUCCESS"));

        ArgumentCaptor<TransactionDefinition> transactionDefinitionCaptor =
            ArgumentCaptor.forClass(TransactionDefinition.class);

        verify(transactionManager).getTransaction(transactionDefinitionCaptor.capture());

        TransactionDefinition transactionDefinition = transactionDefinitionCaptor.getValue();

        assertThat(transactionDefinition.getPropagationBehavior())
            .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(transactionTenantIds).containsExactly(TENANT_ID);
        assertThat(savedTenantIds).containsExactly(TENANT_ID);
    }

    private static AuditEvent createAuditEvent(String principal, String type) {
        return new AuditEvent(principal, type, Map.of("sessionId", "session"));
    }

    private CustomAuditEventRepository createRepository(TenantService tenantService) {
        return createRepository(tenantService, null);
    }

    @SuppressWarnings("unchecked")
    private CustomAuditEventRepository createRepository(
        TenantService tenantService, PlatformTransactionManager transactionManager) {

        ObjectProvider<TenantService> tenantServiceProvider = mock(ObjectProvider.class);

        when(tenantServiceProvider.getIfAvailable()).thenReturn(tenantService);

        ObjectProvider<PlatformTransactionManager> transactionManagerProvider = mock(ObjectProvider.class);

        when(transactionManagerProvider.getIfAvailable()).thenReturn(transactionManager);

        return new CustomAuditEventRepository(
            persistenceAuditEventRepository, new AuditEventConverter(), tenantServiceProvider,
            transactionManagerProvider);
    }
}
