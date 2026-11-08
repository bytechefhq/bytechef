/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.cleanup;

import com.bytechef.ee.automation.ai.gateway.service.AiGatewayWorkspaceSettingsService;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProjectService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityAlertEventService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalExecutionService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayEmbeddedSettings;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProject;
import com.bytechef.ee.platform.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayEmbeddedSettingsService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProjectService;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityAlertEventService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiObservabilityDataCleanupService {

    private static final int DEFAULT_RETENTION_DAYS = 30;

    private static final Logger log = LoggerFactory.getLogger(AiObservabilityDataCleanupService.class);

    private final AiEvalExecutionService aiEvalExecutionService;
    private final AiGatewayMetrics aiGatewayMetrics;
    private final AiGatewayProjectService aiGatewayProjectService;
    private final AiLlmUsageService aiGatewayRequestLogService;
    private final AiGatewayWorkspaceSettingsService aiGatewayWorkspaceSettingsService;
    private final AiObservabilityAlertEventService aiObservabilityAlertEventService;
    private final WorkspaceAiGatewayProjectService workspaceAiGatewayProjectService;
    private final WorkspaceAiObservabilityAlertEventService workspaceAiObservabilityAlertEventService;
    private final WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;
    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final ObjectProvider<AiGatewayEmbeddedSettingsService> embeddedSettingsServiceProvider;

    @SuppressFBWarnings("EI2")
    public AiObservabilityDataCleanupService(
        AiEvalExecutionService aiEvalExecutionService,
        AiGatewayMetrics aiGatewayMetrics,
        AiGatewayProjectService aiGatewayProjectService,
        AiLlmUsageService aiGatewayRequestLogService,
        AiGatewayWorkspaceSettingsService aiGatewayWorkspaceSettingsService,
        AiObservabilityAlertEventService aiObservabilityAlertEventService,
        WorkspaceAiGatewayProjectService workspaceAiGatewayProjectService,
        WorkspaceAiObservabilityAlertEventService workspaceAiObservabilityAlertEventService,
        WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService,
        AiObservabilityTraceService aiObservabilityTraceService,
        ObjectProvider<AiGatewayEmbeddedSettingsService> embeddedSettingsServiceProvider) {

        this.embeddedSettingsServiceProvider = embeddedSettingsServiceProvider;
        this.aiEvalExecutionService = aiEvalExecutionService;
        this.aiGatewayMetrics = aiGatewayMetrics;
        this.aiGatewayProjectService = aiGatewayProjectService;
        this.aiGatewayRequestLogService = aiGatewayRequestLogService;
        this.aiGatewayWorkspaceSettingsService = aiGatewayWorkspaceSettingsService;
        this.aiObservabilityAlertEventService = aiObservabilityAlertEventService;
        this.workspaceAiGatewayProjectService = workspaceAiGatewayProjectService;
        this.workspaceAiObservabilityAlertEventService = workspaceAiObservabilityAlertEventService;
        this.workspaceAiObservabilityTraceService = workspaceAiObservabilityTraceService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanup() {
        log.info("Starting AI observability data cleanup");

        Map<Long, Integer> workspaceRetentions = resolveWorkspaceRetentions();

        for (Map.Entry<Long, Integer> entry : workspaceRetentions.entrySet()) {
            Long workspaceId = entry.getKey();
            int retentionDays = entry.getValue();

            Instant cutoff = Instant.now()
                .minus(retentionDays, ChronoUnit.DAYS);

            try {
                aiGatewayRequestLogService.deleteOlderThanByWorkspace(cutoff, workspaceId);
                workspaceAiObservabilityTraceService.deleteOlderThanByWorkspace(cutoff, workspaceId);
                workspaceAiObservabilityAlertEventService.deleteOlderThanByWorkspace(cutoff, workspaceId);
                aiEvalExecutionService.deleteOlderThanByWorkspace(cutoff, workspaceId);

                log.debug(
                    "Cleanup completed for workspace {} (retention: {} days)", workspaceId, retentionDays);
            } catch (Exception exception) {
                log.error("Cleanup failed for workspace {}", workspaceId, exception);

                aiGatewayMetrics.incrementCleanupFailure();
            }
        }

        cleanUpConnectedUserUsageWithoutWorkspace();

        log.info("AI observability data cleanup finished");
    }

    private void cleanUpConnectedUserUsageWithoutWorkspace() {
        try {
            int retentionDays = resolveEmbeddedRetentionDays();

            Instant cutoff = Instant.now()
                .minus(retentionDays, ChronoUnit.DAYS);

            aiGatewayRequestLogService.deleteOlderThanWithoutWorkspace(cutoff);

            log.debug("Cleanup completed for workspace-less usage (retention: {} days)", retentionDays);
        } catch (Exception exception) {
            log.error("Cleanup failed for workspace-less usage", exception);

            aiGatewayMetrics.incrementCleanupFailure();
        }
    }

    private int resolveEmbeddedRetentionDays() {
        AiGatewayEmbeddedSettingsService embeddedSettingsService = embeddedSettingsServiceProvider.getIfAvailable();

        if (embeddedSettingsService == null) {
            return DEFAULT_RETENTION_DAYS;
        }

        return Arrays.stream(Environment.values())
            .mapToInt(environment -> embeddedSettingsService.find(environment.ordinal())
                .map(AiGatewayEmbeddedSettings::logRetentionDays)
                .orElse(DEFAULT_RETENTION_DAYS))
            .max()
            .orElse(DEFAULT_RETENTION_DAYS);
    }

    private Map<Long, Integer> resolveWorkspaceRetentions() {
        Map<Long, Integer> workspaceRetentions = new HashMap<>();

        Map<Long, Long> projectWorkspaceMap = workspaceAiGatewayProjectService.getProjectWorkspaceMap();

        for (AiGatewayProject project : aiGatewayProjectService.getProjects()) {
            Long workspaceId = projectWorkspaceMap.get(project.getId());

            if (workspaceId == null) {
                continue;
            }

            int projectRetention = resolveRetentionDays(project, workspaceId);

            workspaceRetentions.merge(workspaceId, projectRetention, Math::max);
        }

        for (Long workspaceId : aiGatewayRequestLogService.findDistinctWorkspaceIds()) {
            workspaceRetentions.computeIfAbsent(workspaceId, this::resolveWorkspaceFallbackRetention);
        }

        return workspaceRetentions;
    }

    private int resolveWorkspaceFallbackRetention(Long workspaceId) {
        return aiGatewayWorkspaceSettingsService.findByWorkspaceId(workspaceId)
            .map(settings -> settings.logRetentionDays())
            .orElse(DEFAULT_RETENTION_DAYS);
    }

    private int resolveRetentionDays(AiGatewayProject project, Long workspaceId) {
        Integer projectValue = project.getLogRetentionDays();

        if (projectValue != null) {
            return projectValue;
        }

        if (workspaceId != null) {
            Integer workspaceValue = aiGatewayWorkspaceSettingsService.findByWorkspaceId(workspaceId)
                .map(settings -> settings.logRetentionDays())
                .orElse(null);

            if (workspaceValue != null) {
                return workspaceValue;
            }
        }

        return DEFAULT_RETENTION_DAYS;
    }
}
