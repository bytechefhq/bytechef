/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.ee.automation.configuration.remote.grpc.AutomationConfigurationProtoMapper;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectDeploymentWorkflowConnectionResult;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectDeploymentWorkflowRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectDeploymentWorkflowRemoteServiceGrpc.ProjectDeploymentWorkflowRemoteServiceBlockingStub;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectDeploymentWorkflowRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteProjectDeploymentWorkflowServiceClient implements ProjectDeploymentWorkflowService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private final AutomationConfigurationProtoMapper automationConfigurationProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteProjectDeploymentWorkflowServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.automationConfigurationProtoMapper = new AutomationConfigurationProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public List<ProjectDeploymentWorkflow> create(List<ProjectDeploymentWorkflow> projectDeploymentWorkflows) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDeploymentWorkflow create(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProjectDeploymentWorkflowConnection(long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disableProjectDeploymentWorkflowByConnectionId(long connectionId) {
        getStub().disableProjectDeploymentWorkflowByConnectionId(
            IdRequest.newBuilder()
                .setId(connectionId)
                .build());
    }

    @Override
    public Optional<ProjectDeploymentWorkflow> fetchProjectDeploymentWorkflow(
        long projectDeploymentId, String workflowId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProjectDeploymentWorkflowEnabled(long projectDeploymentId, String workflowId) {
        BoolResponse boolResponse = getStub().isProjectDeploymentWorkflowEnabled(
            toRequest(projectDeploymentId, workflowId));

        return boolResponse.getValue();
    }

    @Override
    public ProjectDeploymentWorkflow getProjectDeploymentWorkflow(long projectDeploymentId, String workflowId) {
        return automationConfigurationProtoMapper.toProjectDeploymentWorkflow(
            getStub().getProjectDeploymentWorkflow(toRequest(projectDeploymentId, workflowId)));
    }

    @Override
    public Optional<ProjectDeploymentWorkflowConnection> fetchProjectDeploymentWorkflowConnection(
        long projectDeploymentId, String workflowId, String workflowNodeName, String workflowConnectionKey) {

        ProjectDeploymentWorkflowConnectionResult projectDeploymentWorkflowConnectionResult =
            getStub().fetchProjectDeploymentWorkflowConnection(
                toRequest(projectDeploymentId, workflowId).toBuilder()
                    .setWorkflowNodeName(workflowNodeName)
                    .setWorkflowConnectionKey(workflowConnectionKey)
                    .build());

        if (!projectDeploymentWorkflowConnectionResult.hasConnection()) {
            return Optional.empty();
        }

        return Optional.of(
            automationConfigurationProtoMapper.toProjectDeploymentWorkflowConnection(
                projectDeploymentWorkflowConnectionResult.getConnection()));
    }

    @Override
    public ProjectDeploymentWorkflow getProjectDeploymentWorkflow(long projectDeploymentWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDeploymentWorkflowConnection> getProjectDeploymentWorkflowConnections(
        long projectDeploymentId, String workflowId, String workflowNodeName) {

        return automationConfigurationProtoMapper.toProjectDeploymentWorkflowConnections(
            getStub().getProjectDeploymentWorkflowConnections(
                toRequest(projectDeploymentId, workflowId).toBuilder()
                    .setWorkflowNodeName(workflowNodeName)
                    .build()));
    }

    @Override
    public List<ProjectDeploymentWorkflow> getProjectDeploymentWorkflows(long projectDeploymentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDeploymentWorkflow> getProjectDeploymentWorkflows(List<Long> projectDeploymentIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDeploymentWorkflow> getWorkflowProjectDeploymentWorkflows(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConnectionUsed(long connectionId) {
        BoolResponse boolResponse = getStub().isConnectionUsed(
            IdRequest.newBuilder()
                .setId(connectionId)
                .build());

        return boolResponse.getValue();
    }

    @Override
    public ProjectDeploymentWorkflow update(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDeploymentWorkflow> update(List<ProjectDeploymentWorkflow> projectDeploymentWorkflows) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearInputs(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(Long id, boolean enable) {
        throw new UnsupportedOperationException();
    }

    private ProjectDeploymentWorkflowRemoteServiceBlockingStub getStub() {
        return ProjectDeploymentWorkflowRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }

    private static ProjectDeploymentWorkflowRequest toRequest(long projectDeploymentId, String workflowId) {
        return ProjectDeploymentWorkflowRequest.newBuilder()
            .setProjectDeploymentId(projectDeploymentId)
            .setWorkflowId(workflowId)
            .build();
    }
}
