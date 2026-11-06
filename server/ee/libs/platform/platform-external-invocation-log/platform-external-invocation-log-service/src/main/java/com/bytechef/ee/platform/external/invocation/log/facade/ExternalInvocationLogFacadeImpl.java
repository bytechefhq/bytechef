/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.dto.ExternalInvocationLogDTO;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
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
public class ExternalInvocationLogFacadeImpl implements ExternalInvocationLogFacade {

    private static final String IS_PLATFORM_USER =
        "!T(com.bytechef.platform.security.web.authentication.NonPlatformUserAuthentication).isCurrent()";

    private final JobService jobService;
    private final ExternalInvocationLogService externalInvocationLogService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ExternalInvocationLogFacadeImpl(
        JobService jobService, ExternalInvocationLogService externalInvocationLogService,
        WorkflowService workflowService) {

        this.jobService = jobService;
        this.externalInvocationLogService = externalInvocationLogService;
        this.workflowService = workflowService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public Page<ExternalInvocationLogDTO> getExternalInvocationLogs(
        PlatformType type, @Nullable Long environmentId, @Nullable ExternalInvocationSurface surface,
        @Nullable ExternalInvocationOutcome outcome,
        @Nullable Long mcpServerId, @Nullable Long a2aServerId, @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId,
        @Nullable Instant startDate, @Nullable Instant endDate, int pageNumber) {

        return toExternalInvocationLogDTOPage(
            externalInvocationLogService.getExternalInvocationLogs(
                type, environmentId == null ? null : Math.toIntExact(environmentId), surface, outcome, mcpServerId,
                a2aServerId,
                connectedUserId, integrationInstanceId, startDate, endDate, pageNumber));
    }

    @Override
    @PreAuthorize(IS_PLATFORM_USER + " and hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')")
    public Page<ExternalInvocationLogDTO> getWorkspaceExternalInvocationLogs(
        PlatformType type, long workspaceId, long environmentId, @Nullable ExternalInvocationSurface surface,
        @Nullable ExternalInvocationOutcome outcome, @Nullable Long mcpServerId, @Nullable Long a2aServerId,
        @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Instant startDate, @Nullable Instant endDate,
        int pageNumber) {

        return toExternalInvocationLogDTOPage(
            externalInvocationLogService.getWorkspaceExternalInvocationLogs(
                type, workspaceId, Math.toIntExact(environmentId), surface, outcome, mcpServerId, a2aServerId,
                connectedUserId,
                integrationInstanceId, startDate, endDate, pageNumber));
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

    private Page<ExternalInvocationLogDTO>
        toExternalInvocationLogDTOPage(Page<ExternalInvocationLog> externalInvocationLogPage) {
        List<Long> jobIds = externalInvocationLogPage.stream()
            .map(ExternalInvocationLog::getJobId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<Long, String> workflowNamesByJobId = getWorkflowNamesByJobId(jobIds);

        return externalInvocationLogPage.map(externalInvocationLog -> {
            Long jobId = externalInvocationLog.getJobId();

            return new ExternalInvocationLogDTO(externalInvocationLog,
                jobId == null ? null : workflowNamesByJobId.get(jobId));
        });
    }
}
