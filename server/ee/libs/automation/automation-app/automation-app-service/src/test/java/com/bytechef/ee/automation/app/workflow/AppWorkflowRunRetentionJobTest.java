/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.service.AppWorkflowService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AppWorkflowRunRetentionJobTest {

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testPurgeDeletesRunsOlderThanRetentionWindow() {
        AppWorkflowService appWorkflowService = mock(AppWorkflowService.class);
        TenantService tenantService = mock(TenantService.class);

        when(tenantService.getTenantIds()).thenReturn(List.of(TenantContext.DEFAULT_TENANT_ID));
        when(appWorkflowService.deleteRunsCreatedBefore(any())).thenReturn(3);

        AppWorkflowRunRetentionJob retentionJob = new AppWorkflowRunRetentionJob(
            applicationProperties(30), appWorkflowService, tenantServiceProvider(tenantService));

        Instant before = Instant.now()
            .minus(30, ChronoUnit.DAYS);

        retentionJob.purgeExpiredRuns();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);

        verify(appWorkflowService).deleteRunsCreatedBefore(captor.capture());

        Instant after = Instant.now()
            .minus(30, ChronoUnit.DAYS);

        assertThat(captor.getValue()).isBetween(before.minusSeconds(5), after);
    }

    @Test
    void testPurgeIsSkippedWhenRetentionIsNotConfigured() {
        AppWorkflowService appWorkflowService = mock(AppWorkflowService.class);
        TenantService tenantService = mock(TenantService.class);

        AppWorkflowRunRetentionJob retentionJob = new AppWorkflowRunRetentionJob(
            applicationProperties(0), appWorkflowService, tenantServiceProvider(tenantService));

        retentionJob.purgeExpiredRuns();

        verify(appWorkflowService, never()).deleteRunsCreatedBefore(any());
        verifyNoInteractions(tenantService);
    }

    @Test
    void testPurgeSweepsEveryTenantAndAFailingTenantDoesNotStopTheRest() {
        AppWorkflowService appWorkflowService = mock(AppWorkflowService.class);
        TenantService tenantService = mock(TenantService.class);
        List<String> sweptTenantIds = new ArrayList<>();

        when(tenantService.getTenantIds()).thenReturn(List.of("tenant1", "tenant2"));
        when(appWorkflowService.deleteRunsCreatedBefore(any())).thenAnswer(invocation -> {
            String tenantId = TenantContext.getCurrentTenantId();

            sweptTenantIds.add(tenantId);

            if ("tenant1".equals(tenantId)) {
                throw new IllegalStateException("schema unavailable");
            }

            return 2;
        });

        AppWorkflowRunRetentionJob retentionJob = new AppWorkflowRunRetentionJob(
            applicationProperties(30), appWorkflowService, tenantServiceProvider(tenantService));

        retentionJob.purgeExpiredRuns();

        assertThat(sweptTenantIds).containsExactly("tenant1", "tenant2");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testPurgeRunsInTheCurrentTenantWhenNoTenantServiceIsAvailable() {
        AppWorkflowService appWorkflowService = mock(AppWorkflowService.class);

        AppWorkflowRunRetentionJob retentionJob = new AppWorkflowRunRetentionJob(
            applicationProperties(30), appWorkflowService, tenantServiceProvider(null));

        retentionJob.purgeExpiredRuns();

        verify(appWorkflowService, times(1)).deleteRunsCreatedBefore(any());
    }

    private static ApplicationProperties applicationProperties(long retentionDays) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.App.WorkflowRun workflowRun = new ApplicationProperties.App.WorkflowRun();

        workflowRun.setRunRetentionDays(retentionDays);

        ApplicationProperties.App appProperties = new ApplicationProperties.App();

        appProperties.setWorkflow(workflowRun);

        applicationProperties.setApp(appProperties);

        return applicationProperties;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<TenantService> tenantServiceProvider(@Nullable TenantService tenantService) {
        ObjectProvider<TenantService> tenantServiceProvider = mock(ObjectProvider.class);

        when(tenantServiceProvider.getIfAvailable()).thenReturn(tenantService);

        return tenantServiceProvider;
    }
}
