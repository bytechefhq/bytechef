/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.grpc;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProject;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectDeployment;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectDeploymentWorkflow;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectDeploymentWorkflowConnection;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectDeploymentWorkflowConnections;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectDeployments;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectWorkflow;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectWorkflowDTO;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectWorkflowDTOs;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectWorkflows;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjects;
import com.bytechef.ee.platform.configuration.remote.grpc.PlatformConfigurationProtoMapper;
import com.bytechef.ee.remote.grpc.ProtoJsonMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

/**
 * Maps projects, project deployments and their workflows to their proto messages and back through
 * {@link ProtoJsonMapper}, leaving out the properties derived from others.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AutomationConfigurationProtoMapper {

    private final PlatformConfigurationProtoMapper platformConfigurationProtoMapper;
    private final ProtoJsonMapper protoJsonMapper;

    public AutomationConfigurationProtoMapper(ObjectMapper objectMapper) {
        this.platformConfigurationProtoMapper = new PlatformConfigurationProtoMapper(objectMapper);
        this.protoJsonMapper = new ProtoJsonMapper(
            objectMapper.rebuild()
                .addMixIn(Project.class, ProjectMixIn.class)
                .addMixIn(ProjectDeployment.class, ProjectDeploymentMixIn.class)
                .addMixIn(ProjectDeploymentWorkflow.class, ProjectDeploymentWorkflowMixIn.class)
                .addMixIn(
                    ProjectDeploymentWorkflowConnection.class, ProjectDeploymentWorkflowConnectionMixIn.class)
                .addMixIn(ProjectVersion.class, NoArgsCreatorMixIn.class)
                .addMixIn(ProjectWorkflow.class, ProjectWorkflowMixIn.class)
                .build());
    }

    public Project toProject(RemoteProject remoteProject) {
        return protoJsonMapper.toObject(remoteProject, Project.class);
    }

    public ProjectDeployment toProjectDeployment(RemoteProjectDeployment remoteProjectDeployment) {
        return protoJsonMapper.toObject(remoteProjectDeployment, ProjectDeployment.class);
    }

    public ProjectDeploymentWorkflow toProjectDeploymentWorkflow(
        RemoteProjectDeploymentWorkflow remoteProjectDeploymentWorkflow) {

        return protoJsonMapper.toObject(remoteProjectDeploymentWorkflow, ProjectDeploymentWorkflow.class);
    }

    public ProjectDeploymentWorkflowConnection toProjectDeploymentWorkflowConnection(
        RemoteProjectDeploymentWorkflowConnection remoteProjectDeploymentWorkflowConnection) {

        return protoJsonMapper.toObject(
            remoteProjectDeploymentWorkflowConnection, ProjectDeploymentWorkflowConnection.class);
    }

    public List<ProjectDeploymentWorkflowConnection> toProjectDeploymentWorkflowConnections(
        RemoteProjectDeploymentWorkflowConnections remoteProjectDeploymentWorkflowConnections) {

        return remoteProjectDeploymentWorkflowConnections.getConnectionsList()
            .stream()
            .map(this::toProjectDeploymentWorkflowConnection)
            .toList();
    }

    public List<ProjectDeployment> toProjectDeployments(RemoteProjectDeployments remoteProjectDeployments) {
        return remoteProjectDeployments.getProjectDeploymentsList()
            .stream()
            .map(this::toProjectDeployment)
            .toList();
    }

    public ProjectWorkflow toProjectWorkflow(RemoteProjectWorkflow remoteProjectWorkflow) {
        return protoJsonMapper.toObject(remoteProjectWorkflow, ProjectWorkflow.class);
    }

    public ProjectWorkflowDTO toProjectWorkflowDTO(RemoteProjectWorkflowDTO remoteProjectWorkflowDTO) {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow(remoteProjectWorkflowDTO.getProjectWorkflowId());

        projectWorkflow.setProjectVersion(remoteProjectWorkflowDTO.getProjectVersion());

        if (remoteProjectWorkflowDTO.hasWorkflowUuid()) {
            projectWorkflow.setUuid(remoteProjectWorkflowDTO.getWorkflowUuid());
        }

        Workflow workflow = platformConfigurationProtoMapper.toWorkflow(remoteProjectWorkflowDTO.getWorkflow());

        return new ProjectWorkflowDTO(workflow, projectWorkflow, remoteProjectWorkflowDTO.getSseStreamResponse());
    }

    public List<ProjectWorkflowDTO> toProjectWorkflowDTOs(RemoteProjectWorkflowDTOs remoteProjectWorkflowDTOs) {
        return remoteProjectWorkflowDTOs.getProjectWorkflowsList()
            .stream()
            .map(this::toProjectWorkflowDTO)
            .toList();
    }

    public List<ProjectWorkflow> toProjectWorkflows(RemoteProjectWorkflows remoteProjectWorkflows) {
        return remoteProjectWorkflows.getProjectWorkflowsList()
            .stream()
            .map(this::toProjectWorkflow)
            .toList();
    }

    public List<Project> toProjects(RemoteProjects remoteProjects) {
        return remoteProjects.getProjectsList()
            .stream()
            .map(this::toProject)
            .toList();
    }

    public RemoteProject toProto(Project project) {
        return protoJsonMapper.toProto(project, RemoteProject.newBuilder());
    }

    public RemoteProjectDeployment toProto(ProjectDeployment projectDeployment) {
        return protoJsonMapper.toProto(projectDeployment, RemoteProjectDeployment.newBuilder());
    }

    public RemoteProjectDeploymentWorkflow toProto(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        return protoJsonMapper.toProto(projectDeploymentWorkflow, RemoteProjectDeploymentWorkflow.newBuilder());
    }

    public RemoteProjectDeploymentWorkflowConnection toProto(
        ProjectDeploymentWorkflowConnection projectDeploymentWorkflowConnection) {

        return protoJsonMapper.toProto(
            projectDeploymentWorkflowConnection, RemoteProjectDeploymentWorkflowConnection.newBuilder());
    }

    public RemoteProjectWorkflow toProto(ProjectWorkflow projectWorkflow) {
        return protoJsonMapper.toProto(projectWorkflow, RemoteProjectWorkflow.newBuilder());
    }

    public RemoteProjectWorkflowDTO toProto(ProjectWorkflowDTO projectWorkflowDTO) {
        RemoteProjectWorkflowDTO.Builder builder = RemoteProjectWorkflowDTO.newBuilder()
            .setProjectVersion(projectWorkflowDTO.getProjectVersion())
            .setProjectWorkflowId(projectWorkflowDTO.getProjectWorkflowId())
            .setSseStreamResponse(projectWorkflowDTO.isSseStreamResponse())
            .setWorkflow(platformConfigurationProtoMapper.toProto(projectWorkflowDTO.getWorkflow()));

        String workflowUuid = projectWorkflowDTO.getWorkflowUuid();

        if (workflowUuid != null) {
            builder.setWorkflowUuid(workflowUuid);
        }

        return builder.build();
    }

    public RemoteProjectDeploymentWorkflowConnections toRemoteProjectDeploymentWorkflowConnections(
        List<ProjectDeploymentWorkflowConnection> projectDeploymentWorkflowConnections) {

        RemoteProjectDeploymentWorkflowConnections.Builder builder =
            RemoteProjectDeploymentWorkflowConnections.newBuilder();

        for (ProjectDeploymentWorkflowConnection projectDeploymentWorkflowConnection : projectDeploymentWorkflowConnections) {

            builder.addConnections(toProto(projectDeploymentWorkflowConnection));
        }

        return builder.build();
    }

    public RemoteProjectDeployments toRemoteProjectDeployments(List<ProjectDeployment> projectDeployments) {
        RemoteProjectDeployments.Builder builder = RemoteProjectDeployments.newBuilder();

        for (ProjectDeployment projectDeployment : projectDeployments) {
            builder.addProjectDeployments(toProto(projectDeployment));
        }

        return builder.build();
    }

    public RemoteProjectWorkflowDTOs toRemoteProjectWorkflowDTOs(List<ProjectWorkflowDTO> projectWorkflowDTOs) {
        RemoteProjectWorkflowDTOs.Builder builder = RemoteProjectWorkflowDTOs.newBuilder();

        for (ProjectWorkflowDTO projectWorkflowDTO : projectWorkflowDTOs) {
            builder.addProjectWorkflows(toProto(projectWorkflowDTO));
        }

        return builder.build();
    }

    public RemoteProjectWorkflows toRemoteProjectWorkflows(List<ProjectWorkflow> projectWorkflows) {
        RemoteProjectWorkflows.Builder builder = RemoteProjectWorkflows.newBuilder();

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            builder.addProjectWorkflows(toProto(projectWorkflow));
        }

        return builder.build();
    }

    public RemoteProjects toRemoteProjects(List<Project> projects) {
        RemoteProjects.Builder builder = RemoteProjects.newBuilder();

        for (Project project : projects) {
            builder.addProjects(toProto(project));
        }

        return builder.build();
    }

    @JsonIgnoreProperties(
        value = {
            "lastProjectVersion", "lastPublishedDate", "lastPublishedProjectVersion", "lastStatus", "published"
        })
    private abstract static class ProjectMixIn {

        @JsonCreator
        ProjectMixIn() {
        }
    }

    @JsonIgnoreProperties("environmentId")
    private abstract static class ProjectDeploymentMixIn {
    }

    private abstract static class NoArgsCreatorMixIn {

        @JsonCreator
        NoArgsCreatorMixIn() {
        }
    }

    @JsonIgnoreProperties("connectionsCount")
    private abstract static class ProjectDeploymentWorkflowMixIn {
    }

    @JsonIgnoreProperties("uuidAsString")
    private abstract static class ProjectWorkflowMixIn {

        @JsonCreator
        ProjectWorkflowMixIn() {
        }
    }

    private abstract static class ProjectDeploymentWorkflowConnectionMixIn {

        @JsonCreator
        @SuppressWarnings("PMD.UnusedFormalParameter")
        ProjectDeploymentWorkflowConnectionMixIn(
            @JsonProperty("connectionId") long connectionId,
            @JsonProperty("workflowConnectionKey") String workflowConnectionKey,
            @JsonProperty("workflowNodeName") String workflowNodeName) {
        }
    }
}
