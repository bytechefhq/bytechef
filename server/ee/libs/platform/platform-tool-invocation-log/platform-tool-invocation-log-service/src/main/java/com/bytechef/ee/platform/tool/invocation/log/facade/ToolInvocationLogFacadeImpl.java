/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.ee.platform.tool.invocation.log.facade.dto.ToolInvocationLogDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ToolInvocationLogFacadeImpl implements ToolInvocationLogFacade {

    private static final String IS_PLATFORM_USER =
        "!T(com.bytechef.platform.security.web.authentication.NonPlatformUserAuthentication).isCurrent()";

    private final JobService jobService;
    private final ToolInvocationLogService toolInvocationLogService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ToolInvocationLogFacadeImpl(
        JobService jobService, ToolInvocationLogService toolInvocationLogService, WorkflowService workflowService) {

        this.jobService = jobService;
        this.toolInvocationLogService = toolInvocationLogService;
        this.workflowService = workflowService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public Page<ToolInvocationLogDTO> getToolInvocationLogs(
        PlatformType type, @Nullable Long environmentId, @Nullable ToolExecutionSurface surface,
        @Nullable ToolExecutionOutcome outcome,
        @Nullable Long mcpServerId, @Nullable Long connectedUserId, @Nullable Long integrationInstanceId,
        @Nullable Long appId, @Nullable Instant startDate, @Nullable Instant endDate, int pageNumber) {

        return toToolInvocationLogDTOPage(
            toolInvocationLogService.getToolInvocationLogs(
                type, environmentId == null ? null : Math.toIntExact(environmentId), surface, outcome, mcpServerId,
                connectedUserId, integrationInstanceId, appId, startDate, endDate, pageNumber));
    }

    @Override
    @PreAuthorize(IS_PLATFORM_USER + " and hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')")
    public Page<ToolInvocationLogDTO> getWorkspaceToolInvocationLogs(
        PlatformType type, long workspaceId, long environmentId, @Nullable ToolExecutionSurface surface,
        @Nullable ToolExecutionOutcome outcome, @Nullable Long mcpServerId, @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Long appId, @Nullable Instant startDate,
        @Nullable Instant endDate, int pageNumber) {

        return toToolInvocationLogDTOPage(
            toolInvocationLogService.getWorkspaceToolInvocationLogs(
                type, workspaceId, Math.toIntExact(environmentId), surface, outcome, mcpServerId, connectedUserId,
                integrationInstanceId, appId, startDate, endDate, pageNumber));
    }

    private Map<Long, String> getWorkflowNamesByJobId(List<Long> jobIds) {
        if (jobIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Optional<Workflow>> workflowsById = new HashMap<>();
        Map<Long, String> workflowNamesByJobId = new HashMap<>();

        for (Job job : jobService.getJobs(jobIds)) {
            String workflowId = job.getWorkflowId();

            if (workflowId == null) {
                continue;
            }

            Optional<Workflow> workflowOptional = workflowsById.computeIfAbsent(
                workflowId, workflowService::fetchWorkflow);

            workflowOptional
                .map(Workflow::getLabel)
                .ifPresent(label -> workflowNamesByJobId.put(job.getId(), label));
        }

        return workflowNamesByJobId;
    }

    private Page<ToolInvocationLogDTO> toToolInvocationLogDTOPage(Page<ToolInvocationLog> toolInvocationLogPage) {
        List<Long> jobIds = toolInvocationLogPage.stream()
            .map(ToolInvocationLog::getJobId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<Long, String> workflowNamesByJobId = getWorkflowNamesByJobId(jobIds);

        return toolInvocationLogPage.map(toolInvocationLog -> {
            Long jobId = toolInvocationLog.getJobId();

            return new ToolInvocationLogDTO(toolInvocationLog, jobId == null ? null : workflowNamesByJobId.get(jobId));
        });
    }
}
