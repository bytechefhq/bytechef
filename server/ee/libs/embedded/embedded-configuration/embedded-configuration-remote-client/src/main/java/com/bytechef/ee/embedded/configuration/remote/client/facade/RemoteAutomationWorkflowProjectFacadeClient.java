/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationReleaseDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.AutomationWorkflowProjectFacadeRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import com.google.protobuf.Empty;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteAutomationWorkflowProjectFacadeClient implements AutomationWorkflowProjectFacade {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteAutomationWorkflowProjectFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.embeddedConfigurationProtoMapper = new EmbeddedConfigurationProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public long createProject(
        String name, String description, String category, List<String> tags, String permissionExpression) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String duplicateProjectWorkflow(String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long duplicateProject(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionWorkflowChange getProjectVersionWorkflowChange(
        long projectId, int projectVersion, String workflowUuid) {

        throw new UnsupportedOperationException();
    }

    @Override
    public VersionWorkflowChanges getProjectVersionWorkflowChanges(long projectId, int projectVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationWorkflowProjectVersionDTO> getProjectVersions(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationWorkflowProjectCategoryDTO> getCategories() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationWorkflowProjectTagDTO> getTags() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String createProjectWorkflow(long projectId, String definition, String permissionExpression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProject(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProjectWorkflow(String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AutomationWorkflowProjectDTO getProject(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getProjects() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getProjects(long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getPublishedProjects() {
        AutomationWorkflowProjectFacadeRemoteServiceGrpc.AutomationWorkflowProjectFacadeRemoteServiceBlockingStub stub =
            AutomationWorkflowProjectFacadeRemoteServiceGrpc
                .newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));

        return embeddedConfigurationProtoMapper.toAutomationWorkflowProjectDTOs(
            stub.getPublishedProjects(Empty.getDefaultInstance()));
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getPublishedProjects(String externalUserId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationWorkflowProjectConfigurationReleaseDTO> getPublishedProjectConfigurations(
        String externalUserId, Environment environment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public int publishProject(long projectId, @Nullable String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProjectWorkflow(String workflowUuid, String label, String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProjectWorkflowPermissionExpression(String workflowUuid, String permissionExpression) {
        throw new UnsupportedOperationException();
    }
}
