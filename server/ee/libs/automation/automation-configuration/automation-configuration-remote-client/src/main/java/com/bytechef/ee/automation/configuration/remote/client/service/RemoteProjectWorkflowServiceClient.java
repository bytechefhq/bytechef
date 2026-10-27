/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.remote.grpc.AutomationConfigurationProtoMapper;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdsRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.OptionalStringResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectWorkflowRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectWorkflowUuidRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.StringResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.StringsResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.WorkflowIdsRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.WorkflowUuidRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteProjectWorkflowServiceClient implements ProjectWorkflowService {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final AutomationConfigurationProtoMapper automationConfigurationProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteProjectWorkflowServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.automationConfigurationProtoMapper = new AutomationConfigurationProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public ProjectWorkflow addWorkflow(long projectId, int projectVersion, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflow addWorkflow(long projectId, int projectVersion, String workflowId, UUID uuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> fetchLastProjectWorkflowId(Long projectId, String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ProjectWorkflow> fetchProjectWorkflow(
        long projectId, int projectVersion, String workflowUuid) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> fetchProjectWorkflowWorkflowId(long projectDeploymentId, String workflowUuid) {
        OptionalStringResponse optionalStringResponse = getStub().fetchProjectWorkflowWorkflowId(
            WorkflowUuidRequest.newBuilder()
                .setProjectDeploymentId(projectDeploymentId)
                .setWorkflowUuid(workflowUuid)
                .build());

        return optionalStringResponse.hasValue() ? Optional.of(optionalStringResponse.getValue()) : Optional.empty();
    }

    @Override
    public String getLastWorkflowId(String workflowUuid) {
        StringResponse stringResponse = getStub().getLastWorkflowId(
            WorkflowUuidRequest.newBuilder()
                .setWorkflowUuid(workflowUuid)
                .build());

        return stringResponse.getValue();
    }

    @Override
    public List<ProjectWorkflow> getLatestProjectWorkflows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflow getLastProjectWorkflow(long projectId, String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLastPublishedWorkflowId(String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflow getProjectWorkflow(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProjectWorkflowWorkflowId(long projectDeploymentId, String workflowUuid) {
        StringResponse stringResponse = getStub().getProjectWorkflowWorkflowId(
            WorkflowUuidRequest.newBuilder()
                .setProjectDeploymentId(projectDeploymentId)
                .setWorkflowUuid(workflowUuid)
                .build());

        return stringResponse.getValue();
    }

    @Override
    public List<Long> getProjectProjectWorkflowIds(long projectId, int projectVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProjectWorkflowUuid(long projectDeploymentId, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getProjectWorkflowIds(long projectId) {
        StringsResponse stringsResponse = getStub().getProjectWorkflowIds(
            IdRequest.newBuilder()
                .setId(projectId)
                .build());

        return List.copyOf(stringsResponse.getValuesList());
    }

    @Override
    public List<String> getProjectWorkflowIds(long projectId, int projectVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows(List<Long> projectIds) {
        return automationConfigurationProtoMapper.toProjectWorkflows(
            getStub().getProjectsProjectWorkflows(
                IdsRequest.newBuilder()
                    .addAllIds(projectIds)
                    .build()));
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows(long projectId, int lastVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows(Long projectId, String workflowUuid) {
        ProjectWorkflowUuidRequest.Builder builder = ProjectWorkflowUuidRequest.newBuilder()
            .setWorkflowUuid(workflowUuid);

        if (projectId != null) {
            builder.setProjectId(projectId);
        }

        return automationConfigurationProtoMapper.toProjectWorkflows(
            getStub().getProjectUuidProjectWorkflows(builder.build()));
    }

    @Override
    public ProjectWorkflow getWorkflowProjectWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflow> getWorkflowProjectWorkflows(List<String> workflowIds) {
        return automationConfigurationProtoMapper.toProjectWorkflows(
            getStub().getWorkflowProjectWorkflows(
                WorkflowIdsRequest.newBuilder()
                    .addAllWorkflowIds(workflowIds)
                    .build()));
    }

    @Override
    public List<ProjectWorkflow> getWorkflowUuidProjectWorkflows(String workflowUuid) {
        return automationConfigurationProtoMapper.toProjectWorkflows(
            getStub().getWorkflowUuidProjectWorkflows(
                WorkflowUuidRequest.newBuilder()
                    .setWorkflowUuid(workflowUuid)
                    .build()));
    }

    @Override
    public void delete(long projectId, int projectVersion, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void publishWorkflow(
        long projectId, int oldProjectVersion, String oldWorkflowId, ProjectWorkflow projectWorkflow) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflow update(ProjectWorkflow projectWorkflow) {
        throw new UnsupportedOperationException();
    }

    private ProjectWorkflowRemoteServiceGrpc.ProjectWorkflowRemoteServiceBlockingStub getStub() {
        return ProjectWorkflowRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }
}
