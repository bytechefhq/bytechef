/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.remote.grpc;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.ee.automation.configuration.remote.grpc.AutomationConfigurationProtoMapper;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.proto.GetWorkflowExecutionsRequest;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.proto.RemoteWorkflowExecution;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.proto.RemoteWorkflowExecutionPage;
import com.bytechef.ee.platform.configuration.remote.grpc.PlatformConfigurationProtoMapper;
import com.bytechef.ee.remote.grpc.JsonBytes;
import com.bytechef.platform.workflow.execution.dto.FocusPathElementDTO;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TriggerExecutionDTO;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkflowExecutionProtoMapper {

    private static final TypeReference<List<FocusPathElementDTO>> FOCUS_PATH_TYPE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<JobDTO> JOB_DTO_TYPE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<TriggerExecutionDTO> TRIGGER_EXECUTION_DTO_TYPE_REFERENCE =
        new TypeReference<>() {};

    private final AutomationConfigurationProtoMapper automationConfigurationProtoMapper;
    private final JsonBytes jsonBytes;
    private final PlatformConfigurationProtoMapper platformConfigurationProtoMapper;

    public WorkflowExecutionProtoMapper(ObjectMapper objectMapper) {
        this.automationConfigurationProtoMapper = new AutomationConfigurationProtoMapper(objectMapper);
        this.jsonBytes = new JsonBytes(objectMapper);
        this.platformConfigurationProtoMapper = new PlatformConfigurationProtoMapper(objectMapper);
    }

    public Page<WorkflowExecutionDTO> toPage(RemoteWorkflowExecutionPage remoteWorkflowExecutionPage) {
        List<WorkflowExecutionDTO> workflowExecutionDTOs = remoteWorkflowExecutionPage.getContentList()
            .stream()
            .map(this::toWorkflowExecutionDTO)
            .toList();

        int pageSize = remoteWorkflowExecutionPage.getPageSize();

        Pageable pageable = pageSize > 0
            ? PageRequest.of(remoteWorkflowExecutionPage.getPageNumber(), pageSize) : Pageable.unpaged();

        return new PageImpl<>(workflowExecutionDTOs, pageable, remoteWorkflowExecutionPage.getTotalElements());
    }

    public RemoteWorkflowExecution toProto(WorkflowExecutionDTO workflowExecutionDTO) {
        RemoteWorkflowExecution.Builder builder = RemoteWorkflowExecution.newBuilder()
            .setId(workflowExecutionDTO.id())
            .setJob(jsonBytes.write(workflowExecutionDTO.job()))
            .setTriggerExecution(jsonBytes.write(workflowExecutionDTO.triggerExecution()))
            .setFocusPath(jsonBytes.write(workflowExecutionDTO.focusPath()));

        if (workflowExecutionDTO.project() != null) {
            builder.setProject(automationConfigurationProtoMapper.toProto(workflowExecutionDTO.project()));
        }

        if (workflowExecutionDTO.projectDeployment() != null) {
            builder.setProjectDeployment(
                automationConfigurationProtoMapper.toProto(workflowExecutionDTO.projectDeployment()));
        }

        Integer projectVersion = workflowExecutionDTO.projectVersion();

        if (projectVersion != null) {
            builder.setProjectVersion(projectVersion);
        }

        if (workflowExecutionDTO.workflow() != null) {
            builder.setWorkflow(platformConfigurationProtoMapper.toProto(workflowExecutionDTO.workflow()));
        }

        Long taskExecutionCount = workflowExecutionDTO.taskExecutionCount();

        if (taskExecutionCount != null) {
            builder.setTaskExecutionCount(taskExecutionCount);
        }

        return builder.build();
    }

    public RemoteWorkflowExecutionPage toProto(Page<WorkflowExecutionDTO> page) {
        RemoteWorkflowExecutionPage.Builder builder = RemoteWorkflowExecutionPage.newBuilder()
            .setTotalElements(page.getTotalElements());

        Pageable pageable = page.getPageable();

        if (pageable.isPaged()) {
            builder.setPageNumber(pageable.getPageNumber())
                .setPageSize(pageable.getPageSize());
        }

        for (WorkflowExecutionDTO workflowExecutionDTO : page.getContent()) {
            builder.addContent(toProto(workflowExecutionDTO));
        }

        return builder.build();
    }

    public GetWorkflowExecutionsRequest toProto(
        @Nullable Boolean embedded, @Nullable Long environmentId, Job.@Nullable Status jobStatus,
        @Nullable Instant jobStartDate, @Nullable Instant jobEndDate, @Nullable Long projectId,
        @Nullable Long projectDeploymentId, @Nullable Long connectedUserId, @Nullable String workflowId,
        @Nullable Integer projectVersion, long workspaceId, int pageNumber) {

        GetWorkflowExecutionsRequest.Builder builder = GetWorkflowExecutionsRequest.newBuilder()
            .setWorkspaceId(workspaceId)
            .setPageNumber(pageNumber);

        if (embedded != null) {
            builder.setEmbedded(embedded);
        }

        if (environmentId != null) {
            builder.setEnvironmentId(environmentId);
        }

        if (jobStatus != null) {
            builder.setJobStatus(jobStatus.name());
        }

        if (jobStartDate != null) {
            builder.setJobStartDate(toTimestamp(jobStartDate));
        }

        if (jobEndDate != null) {
            builder.setJobEndDate(toTimestamp(jobEndDate));
        }

        if (projectId != null) {
            builder.setProjectId(projectId);
        }

        if (projectDeploymentId != null) {
            builder.setProjectDeploymentId(projectDeploymentId);
        }

        if (connectedUserId != null) {
            builder.setConnectedUserId(connectedUserId);
        }

        if (workflowId != null) {
            builder.setWorkflowId(workflowId);
        }

        if (projectVersion != null) {
            builder.setProjectVersion(projectVersion);
        }

        return builder.build();
    }

    public @Nullable Instant toInstant(boolean present, Timestamp timestamp) {
        if (!present) {
            return null;
        }

        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    public WorkflowExecutionDTO toWorkflowExecutionDTO(RemoteWorkflowExecution remoteWorkflowExecution) {
        return new WorkflowExecutionDTO(
            remoteWorkflowExecution.getId(),
            remoteWorkflowExecution.hasProject()
                ? automationConfigurationProtoMapper.toProject(remoteWorkflowExecution.getProject()) : null,
            remoteWorkflowExecution.hasProjectDeployment()
                ? automationConfigurationProtoMapper.toProjectDeployment(remoteWorkflowExecution.getProjectDeployment())
                : null,
            remoteWorkflowExecution.hasProjectVersion() ? remoteWorkflowExecution.getProjectVersion() : null,
            jsonBytes.read(remoteWorkflowExecution.getJob(), JOB_DTO_TYPE_REFERENCE),
            remoteWorkflowExecution.hasWorkflow()
                ? platformConfigurationProtoMapper.toWorkflow(remoteWorkflowExecution.getWorkflow()) : null,
            jsonBytes.read(remoteWorkflowExecution.getTriggerExecution(), TRIGGER_EXECUTION_DTO_TYPE_REFERENCE),
            jsonBytes.read(remoteWorkflowExecution.getFocusPath(), FOCUS_PATH_TYPE_REFERENCE),
            remoteWorkflowExecution.hasTaskExecutionCount() ? remoteWorkflowExecution.getTaskExecutionCount() : null);
    }

    private static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }
}
