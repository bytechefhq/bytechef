/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.configuration.remote.grpc.AutomationConfigurationProtoMapper;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectRemoteServiceGrpc.ProjectRemoteServiceBlockingStub;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.WorkflowIdRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.google.protobuf.Empty;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
public class RemoteProjectServiceClient implements ProjectService {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final AutomationConfigurationProtoMapper automationConfigurationProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteProjectServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.automationConfigurationProtoMapper = new AutomationConfigurationProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public long countProjects() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project create(Project project) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Project> fetchProject(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Project> fetchProject(String name, long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project getProjectDeploymentProject(long projectDeploymentId) {
        return automationConfigurationProtoMapper.toProject(
            getStub().getProjectDeploymentProject(toIdRequest(projectDeploymentId)));
    }

    @Override
    public Project getProject(long id) {
        return automationConfigurationProtoMapper.toProject(getStub().getProject(toIdRequest(id)));
    }

    @Override
    public Project getProject(UUID uuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectVersion> getProjectVersions(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Project> getProjects() {
        return automationConfigurationProtoMapper.toProjects(getStub().getProjects(Empty.getDefaultInstance()));
    }

    @Override
    public List<Project> getProjects(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Project> getProjects(
        Boolean apiCollections, Long categoryId, Boolean projectDeployments, Long tagId,
        Status status, Long workspaceId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Project getWorkflowProject(String workflowId) {
        return automationConfigurationProtoMapper.toProject(
            getStub().getWorkflowProject(
                WorkflowIdRequest.newBuilder()
                    .setWorkflowId(workflowId)
                    .build()));
    }

    @Override
    public List<Long> getWorkspaceProjectIds(long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectVersion getPublishedProjectVersion(long id, int projectVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> getWorkspaceProjectIds(long workspaceId, Set<ProjectOwner> excludedProjectOwners) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int publishProject(long id, String description, boolean syncWithGit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProjectVersion(long id, int projectVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project update(Project project) {
        throw new UnsupportedOperationException();
    }

    private ProjectRemoteServiceBlockingStub getStub() {
        return ProjectRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }

    private static IdRequest toIdRequest(long id) {
        return IdRequest.newBuilder()
            .setId(id)
            .build();
    }
}
