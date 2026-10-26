/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.grpc;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProject;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectDeployment;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectDeploymentWorkflow;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectDeploymentWorkflowConnection;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectDeploymentWorkflowConnections;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjects;
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

    private final ProtoJsonMapper protoJsonMapper;

    public AutomationConfigurationProtoMapper(ObjectMapper objectMapper) {
        this.protoJsonMapper = new ProtoJsonMapper(
            objectMapper.rebuild()
                .addMixIn(Project.class, ProjectMixIn.class)
                .addMixIn(ProjectDeployment.class, ProjectDeploymentMixIn.class)
                .addMixIn(ProjectDeploymentWorkflow.class, ProjectDeploymentWorkflowMixIn.class)
                .addMixIn(
                    ProjectDeploymentWorkflowConnection.class, ProjectDeploymentWorkflowConnectionMixIn.class)
                .addMixIn(ProjectVersion.class, NoArgsCreatorMixIn.class)
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

    public RemoteProjectDeploymentWorkflowConnections toRemoteProjectDeploymentWorkflowConnections(
        List<ProjectDeploymentWorkflowConnection> projectDeploymentWorkflowConnections) {

        RemoteProjectDeploymentWorkflowConnections.Builder builder =
            RemoteProjectDeploymentWorkflowConnections.newBuilder();

        for (ProjectDeploymentWorkflowConnection projectDeploymentWorkflowConnection : projectDeploymentWorkflowConnections) {

            builder.addConnections(toProto(projectDeploymentWorkflowConnection));
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
