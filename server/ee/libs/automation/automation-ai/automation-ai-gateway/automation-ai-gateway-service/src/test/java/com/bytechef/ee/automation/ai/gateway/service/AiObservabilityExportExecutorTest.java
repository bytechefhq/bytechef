/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportFormat;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJob;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJobStatus;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJobType;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportScope;
import com.bytechef.ee.automation.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.scheduler.event.ExportExecutionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Unit tests for {@link AiObservabilityExportExecutor}. Focuses on the scheduled-event listener entry point — the
 * {@code @Async executeExport} itself ends up touching file storage, so it's covered by the module integration tests.
 * The listener's CANCELLED-guard and PROCESSING-guard are what prevent duplicate exports when the Quartz scheduler
 * fires rapidly.
 *
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiObservabilityExportExecutorTest {

    @Mock
    private AiGatewayMetrics aiGatewayMetrics;

    @Mock
    private AiObservabilityExportJobService aiObservabilityExportJobService;

    @Mock
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Mock
    private AiObservabilitySessionService aiObservabilitySessionService;

    @Mock
    private AiGatewayRequestLogService aiGatewayRequestLogService;

    @Mock
    private AiPromptService aiPromptService;

    @Mock
    private AiPromptVersionService aiPromptVersionService;

    @Mock
    private FileStorageServiceRegistry fileStorageServiceRegistry;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ObjectProvider<AiObservabilityExportExecutor> selfProvider;

    private AiObservabilityExportExecutor aiObservabilityExportExecutor;

    @BeforeEach
    void setUp() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getFileStorage()
            .setProvider(FileStorage.Provider.JDBC);

        when(fileStorageServiceRegistry.getFileStorageService("JDBC")).thenReturn(fileStorageService);

        aiObservabilityExportExecutor = new AiObservabilityExportExecutor(
            aiGatewayMetrics, aiObservabilityExportJobService, aiObservabilityTraceService,
            aiObservabilitySessionService, aiGatewayRequestLogService, aiPromptService, aiPromptVersionService,
            applicationProperties, fileStorageServiceRegistry, selfProvider);
    }

    @Test
    void testOnScheduledExportSkipsCancelledJob() {
        AiObservabilityExportJob job = newJob();

        job.setStatus(AiObservabilityExportJobStatus.CANCELLED);

        when(aiObservabilityExportJobService.getExportJob(7L)).thenReturn(job);

        aiObservabilityExportExecutor.onScheduledExport(new ExportExecutionEvent(7L));

        verify(aiObservabilityExportJobService, never()).update(any());
        verify(selfProvider, never()).getObject();
    }

    @Test
    void testOnScheduledExportSkipsJobStillProcessing() {
        AiObservabilityExportJob job = newJob();

        job.setStatus(AiObservabilityExportJobStatus.PROCESSING);

        when(aiObservabilityExportJobService.getExportJob(9L)).thenReturn(job);

        aiObservabilityExportExecutor.onScheduledExport(new ExportExecutionEvent(9L));

        verify(aiObservabilityExportJobService, never()).update(any());
        verify(selfProvider, never()).getObject();
    }

    @Test
    void testOnScheduledExportResetsToPendingAndDispatchesViaProxy() {
        AiObservabilityExportJob job = newJob();

        // Start from COMPLETED to prove the listener resets status before dispatching.
        job.setStatus(AiObservabilityExportJobStatus.COMPLETED);

        AiObservabilityExportExecutor proxy = org.mockito.Mockito.mock(AiObservabilityExportExecutor.class);

        when(aiObservabilityExportJobService.getExportJob(21L)).thenReturn(job);
        when(selfProvider.getObject()).thenReturn(proxy);

        aiObservabilityExportExecutor.onScheduledExport(new ExportExecutionEvent(21L));

        // Status reset MUST happen before we dispatch — otherwise a refactor that dispatches first could leave a
        // job visibly COMPLETED while the async worker is already re-running it. InOrder enforces the ordering
        // invariant rather than merely asserting both calls happened.
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(aiObservabilityExportJobService, proxy);

        inOrder.verify(aiObservabilityExportJobService)
            .update(job);
        inOrder.verify(proxy)
            .executeExport(21L);
    }

    @Test
    void testExecuteExportSkipsCancelledJobWithoutTouchingFileStorage() {
        AiObservabilityExportJob job = newJob();

        job.setStatus(AiObservabilityExportJobStatus.CANCELLED);

        when(aiObservabilityExportJobService.getExportJob(11L)).thenReturn(job);

        aiObservabilityExportExecutor.executeExport(11L);

        verify(aiObservabilityExportJobService, never()).update(any());
        verify(fileStorageService, never()).storeFileContent(anyString(), anyString(), anyString());
    }

    private static AiObservabilityExportJob newJob() {
        return new AiObservabilityExportJob(
            1L, AiObservabilityExportJobType.SCHEDULED, AiObservabilityExportFormat.JSON,
            AiObservabilityExportScope.TRACES, "tester");
    }
}
