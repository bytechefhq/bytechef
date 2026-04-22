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
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProject;
import com.bytechef.ee.platform.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProjectService;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityAlertEventService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled cleanup of AI observability data per workspace's effective retention. Runs daily at 3 AM.
 *
 * <p>
 * Scopes every delete by workspaceId so a project with a short retention in workspace A cannot delete data belonging to
 * workspace B. When multiple projects share a workspace with different retentions, the cleanup picks the MAX retention
 * for that workspace so no project ever loses data it wanted to keep — project-specific shorter retentions for
 * per-project tables are a follow-up.
 *
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
        AiObservabilityTraceService aiObservabilityTraceService) {

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

        log.info("AI observability data cleanup finished");
    }

    /**
     * Builds a workspaceId → MAX(retentionDays) map covering every workspace that has data to potentially clean. Uses
     * MAX across a workspace's projects so cleanup never deletes data a project wanted to keep longer. Workspaces with
     * no projects still get cleanup with their workspace setting (or {@link #DEFAULT_RETENTION_DAYS}) — they are
     * discovered via distinct workspaceIds in the request log table.
     */
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

    /**
     * Retention precedence: project.logRetentionDays → workspace settings.logRetentionDays → DEFAULT.
     */
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
