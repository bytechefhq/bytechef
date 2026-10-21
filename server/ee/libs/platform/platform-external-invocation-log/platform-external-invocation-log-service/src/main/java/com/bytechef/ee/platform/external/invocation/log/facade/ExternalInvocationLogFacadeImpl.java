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
import com.bytechef.ee.platform.external.invocation.log.security.ExternalInvocationLogAppInstanceAccess;
import com.bytechef.ee.platform.external.invocation.log.security.ViewableAppInstances;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
import com.bytechef.platform.external.invocation.ExternalInvocationStatus;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
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

    private final ObjectProvider<ExternalInvocationLogAppInstanceAccess> externalInvocationLogAppInstanceAccessProvider;
    private final JobService jobService;
    private final ExternalInvocationLogService externalInvocationLogService;
    private final ObjectProvider<UserService> userServiceProvider;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ExternalInvocationLogFacadeImpl(
        ObjectProvider<ExternalInvocationLogAppInstanceAccess> externalInvocationLogAppInstanceAccessProvider,
        JobService jobService, ExternalInvocationLogService externalInvocationLogService,
        ObjectProvider<UserService> userServiceProvider, WorkflowService workflowService) {

        this.externalInvocationLogAppInstanceAccessProvider = externalInvocationLogAppInstanceAccessProvider;
        this.jobService = jobService;
        this.externalInvocationLogService = externalInvocationLogService;
        this.userServiceProvider = userServiceProvider;
        this.workflowService = workflowService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public Page<ExternalInvocationLogDTO> getExternalInvocationLogs(
        PlatformType type, @Nullable Long environmentId, @Nullable ExternalInvocationSource source,
        @Nullable ExternalInvocationStatus status,
        @Nullable Long mcpServerId, @Nullable Long a2aServerId, @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Long appId,
        @Nullable Instant startDate, @Nullable Instant endDate, int pageNumber) {

        return toExternalInvocationLogDTOPage(
            externalInvocationLogService.getExternalInvocationLogs(
                type, environmentId == null ? null : Math.toIntExact(environmentId), source, status, mcpServerId,
                a2aServerId,
                connectedUserId, integrationInstanceId, appId, startDate, endDate, pageNumber));
    }

    @Override
    @PreAuthorize(IS_PLATFORM_USER + " and hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')")
    public Page<ExternalInvocationLogDTO> getWorkspaceExternalInvocationLogs(
        PlatformType type, long workspaceId, long environmentId, @Nullable ExternalInvocationSource source,
        @Nullable ExternalInvocationStatus status, @Nullable Long mcpServerId, @Nullable Long a2aServerId,
        @Nullable Long connectedUserId,
        @Nullable Long integrationInstanceId, @Nullable Long appId, @Nullable Long appInstanceId,
        @Nullable Instant startDate, @Nullable Instant endDate, int pageNumber) {

        int environment = Math.toIntExact(environmentId);

        return toExternalInvocationLogDTOPage(
            externalInvocationLogService.getWorkspaceExternalInvocationLogs(
                type, workspaceId, environment, source, status, mcpServerId, a2aServerId, connectedUserId,
                integrationInstanceId, appId, appInstanceId, getViewableAppInstances(workspaceId, environment),
                startDate, endDate, pageNumber));
    }

    private static @Nullable String toUserName(User user) {
        String fullName = Stream.of(user.getFirstName(), user.getLastName())
            .filter(namePart -> namePart != null && !namePart.isBlank())
            .collect(Collectors.joining(" "));

        if (!fullName.isEmpty()) {
            return fullName;
        }

        String email = user.getEmail();

        if (email != null && !email.isBlank()) {
            return email;
        }

        return user.getLogin();
    }

    private Map<Long, String> getUserNamesByUserId(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        UserService userService = userServiceProvider.getIfAvailable();

        if (userService == null) {
            return Map.of();
        }

        Map<Long, String> userNamesByUserId = new HashMap<>();

        for (User user : userService.getUsers(userIds)) {
            String userName = toUserName(user);

            if (userName != null) {
                userNamesByUserId.put(user.getId(), userName);
            }
        }

        return userNamesByUserId;
    }

    private ViewableAppInstances getViewableAppInstances(long workspaceId, int environment) {
        ExternalInvocationLogAppInstanceAccess externalInvocationLogAppInstanceAccess =
            externalInvocationLogAppInstanceAccessProvider.getIfAvailable();

        if (externalInvocationLogAppInstanceAccess == null) {
            return ViewableAppInstances.only(Set.of());
        }

        return externalInvocationLogAppInstanceAccess.getViewableAppInstances(workspaceId, environment);
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

        List<Long> userIds = externalInvocationLogPage.stream()
            .map(ExternalInvocationLog::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<Long, String> workflowNamesByJobId = getWorkflowNamesByJobId(jobIds);
        Map<Long, String> userNamesByUserId = getUserNamesByUserId(userIds);

        return externalInvocationLogPage.map(externalInvocationLog -> {
            Long jobId = externalInvocationLog.getJobId();
            Long userId = externalInvocationLog.getUserId();

            return new ExternalInvocationLogDTO(
                externalInvocationLog, jobId == null ? null : workflowNamesByJobId.get(jobId),
                userId == null ? null : userNamesByUserId.get(userId));
        });
    }
}
