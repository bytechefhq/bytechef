/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectWorkflowRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.StringResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.WorkflowUuidRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteProjectWorkflowServiceClient implements ProjectWorkflowService {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final GrpcChannels grpcChannels;

    public RemoteProjectWorkflowServiceClient(GrpcChannels grpcChannels) {
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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflow getWorkflowProjectWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflow> getWorkflowProjectWorkflows(List<String> workflowIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflow> getWorkflowUuidProjectWorkflows(String workflowUuid) {
        throw new UnsupportedOperationException();
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
