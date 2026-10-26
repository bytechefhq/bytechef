/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.dto.AutomationProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationReleaseDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectFacade;
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
public class RemoteAutomationProjectFacadeClient implements AutomationProjectFacade {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteAutomationProjectFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
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
    public List<AutomationProjectVersionDTO> getProjectVersions(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectCategoryDTO> getCategories() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectTagDTO> getTags() {
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
    public AutomationProjectDTO getProject(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectDTO> getProjects() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectDTO> getProjects(long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectDTO> getPublishedProjects() {
        AutomationWorkflowProjectFacadeRemoteServiceGrpc.AutomationWorkflowProjectFacadeRemoteServiceBlockingStub stub =
            AutomationWorkflowProjectFacadeRemoteServiceGrpc
                .newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));

        return embeddedConfigurationProtoMapper.toAutomationWorkflowProjectDTOs(
            stub.getPublishedProjects(Empty.getDefaultInstance()));
    }

    @Override
    public List<AutomationProjectDTO> getPublishedProjects(String externalUserId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectConfigurationReleaseDTO> getPublishedProjectConfigurations(
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
