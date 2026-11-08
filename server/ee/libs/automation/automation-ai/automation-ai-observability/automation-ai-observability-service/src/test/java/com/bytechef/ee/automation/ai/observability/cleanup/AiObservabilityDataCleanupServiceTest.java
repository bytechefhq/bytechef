/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.service.AiGatewayWorkspaceSettingsService;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProjectService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityAlertEventService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalExecutionService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayEmbeddedSettings;
import com.bytechef.ee.platform.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayEmbeddedSettingsService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProjectService;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityAlertEventService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiObservabilityDataCleanupServiceTest {

    @Mock
    private AiEvalExecutionService aiEvalExecutionService;

    @Mock
    private AiGatewayMetrics aiGatewayMetrics;

    @Mock
    private AiGatewayProjectService aiGatewayProjectService;

    @Mock
    private AiLlmUsageService aiLlmUsageService;

    @Mock
    private AiGatewayWorkspaceSettingsService aiGatewayWorkspaceSettingsService;

    @Mock
    private AiObservabilityAlertEventService aiObservabilityAlertEventService;

    @Mock
    private WorkspaceAiGatewayProjectService workspaceAiGatewayProjectService;

    @Mock
    private WorkspaceAiObservabilityAlertEventService workspaceAiObservabilityAlertEventService;

    @Mock
    private WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;

    @Mock
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Mock
    private ObjectProvider<AiGatewayEmbeddedSettingsService> embeddedSettingsServiceProvider;

    @Mock
    private AiGatewayEmbeddedSettingsService embeddedSettingsService;

    private AiObservabilityDataCleanupService cleanupService;

    @BeforeEach
    void beforeEach() {
        cleanupService = new AiObservabilityDataCleanupService(
            aiEvalExecutionService, aiGatewayMetrics, aiGatewayProjectService, aiLlmUsageService,
            aiGatewayWorkspaceSettingsService, aiObservabilityAlertEventService, workspaceAiGatewayProjectService,
            workspaceAiObservabilityAlertEventService, workspaceAiObservabilityTraceService,
            aiObservabilityTraceService, embeddedSettingsServiceProvider);

        when(embeddedSettingsServiceProvider.getIfAvailable()).thenReturn(embeddedSettingsService);
    }

    @Test
    void testWorkspaceLessUsageIsKeptForTheLongestRetentionAnyEnvironmentWants() {
        when(embeddedSettingsService.find(Environment.DEVELOPMENT.ordinal()))
            .thenReturn(Optional.of(settingsWithRetention(Environment.DEVELOPMENT, 7)));
        when(embeddedSettingsService.find(Environment.STAGING.ordinal()))
            .thenReturn(Optional.of(settingsWithRetention(Environment.STAGING, 90)));
        when(embeddedSettingsService.find(Environment.PRODUCTION.ordinal())).thenReturn(Optional.empty());

        cleanupService.cleanup();

        assertThat(captureWorkspaceLessCutoff()).isBetween(daysAgo(91), daysAgo(89));
    }

    @Test
    void testWorkspaceLessUsageFallsBackToTheDefaultRetentionWithoutEmbeddedSettings() {
        when(embeddedSettingsServiceProvider.getIfAvailable()).thenReturn(null);

        cleanupService.cleanup();

        assertThat(captureWorkspaceLessCutoff()).isBetween(daysAgo(31), daysAgo(29));
    }

    @Test
    void testWorkspaceLessCleanupFailureIsCountedAsACleanupFailure() {
        when(embeddedSettingsService.find(any(Long.class))).thenReturn(Optional.empty());

        doThrow(new IllegalStateException("database unavailable")).when(aiLlmUsageService)
            .deleteOlderThanWithoutWorkspace(any());

        cleanupService.cleanup();

        verify(aiGatewayMetrics).incrementCleanupFailure();
    }

    private Instant captureWorkspaceLessCutoff() {
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(aiLlmUsageService).deleteOlderThanWithoutWorkspace(cutoffCaptor.capture());

        return cutoffCaptor.getValue();
    }

    private static Instant daysAgo(int days) {
        return Instant.now()
            .minus(days, ChronoUnit.DAYS);
    }

    private static AiGatewayEmbeddedSettings settingsWithRetention(Environment environment, int logRetentionDays) {
        return new AiGatewayEmbeddedSettings(
            (long) environment.ordinal(), null, null, null, null, logRetentionDays, null, null, null);
    }
}
