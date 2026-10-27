/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.automation.configuration.remote.grpc.AutomationConfigurationProtoMapper;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdsRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectDeploymentRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectDeploymentsFilterRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
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
public class RemoteProjectDeploymentServiceClient implements ProjectDeploymentService {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final AutomationConfigurationProtoMapper automationConfigurationProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteProjectDeploymentServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.automationConfigurationProtoMapper = new AutomationConfigurationProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public ProjectDeployment create(ProjectDeployment projectDeployment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ProjectDeployment> fetchProjectDeployment(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ProjectDeployment> fetchProjectDeployment(long projectId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProjectDeploymentEnabled(long projectDeploymentId) {
        BoolResponse boolResponse = getStub().isProjectDeploymentEnabled(
            IdRequest.newBuilder()
                .setId(projectDeploymentId)
                .build());

        return boolResponse.getValue();
    }

    @Override
    public List<ProjectDeployment> getAllProjectDeployments(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDeployment getProjectDeployment(long id) {
        return automationConfigurationProtoMapper.toProjectDeployment(
            getStub().getProjectDeployment(
                IdRequest.newBuilder()
                    .setId(id)
                    .build()));
    }

    @Override
    public ProjectDeployment getProjectDeployment(long projectId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getProjectDeploymentId(long projectId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDeployment> getProjectDeployments() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDeployment> getProjectDeployments(List<Long> ids) {
        return automationConfigurationProtoMapper.toProjectDeployments(
            getStub().getProjectDeployments(
                IdsRequest.newBuilder()
                    .addAllIds(ids)
                    .build()));
    }

    @Override
    public List<ProjectDeployment> getProjectDeployments(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDeployment> getProjectDeployments(
        Boolean embedded, Environment environment, Long projectId, Long tagId, Long workspaceId) {

        ProjectDeploymentsFilterRequest.Builder builder = ProjectDeploymentsFilterRequest.newBuilder();

        if (embedded != null) {
            builder.setEmbedded(embedded);
        }

        if (environment != null) {
            builder.setEnvironment(environment.name());
        }

        if (projectId != null) {
            builder.setProjectId(projectId);
        }

        if (tagId != null) {
            builder.setTagId(tagId);
        }

        if (workspaceId != null) {
            builder.setWorkspaceId(workspaceId);
        }

        return automationConfigurationProtoMapper.toProjectDeployments(
            getStub().getProjectDeploymentsByFilter(builder.build()));
    }

    @Override
    public ProjectDeployment update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDeployment update(ProjectDeployment projectDeployment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disableAllProjectDeployments() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        throw new UnsupportedOperationException();
    }

    private ProjectDeploymentRemoteServiceGrpc.ProjectDeploymentRemoteServiceBlockingStub getStub() {
        return ProjectDeploymentRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }
}
