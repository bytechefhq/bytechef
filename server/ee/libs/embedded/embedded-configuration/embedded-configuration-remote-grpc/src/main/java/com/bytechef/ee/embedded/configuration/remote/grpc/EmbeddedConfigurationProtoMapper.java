/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteAppEvent;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteAutomationWorkflowProject;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteAutomationWorkflowProjects;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegration;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstance;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceConfigurationWorkflowConnections;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceConfigurationWorkflows;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceConfigurations;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceWorkflows;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstances;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationWorkflowDTOs;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationWorkflows;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrations;
import com.bytechef.ee.platform.configuration.remote.grpc.PlatformConfigurationProtoMapper;
import com.bytechef.ee.remote.grpc.ProtoJsonMapper;
import com.bytechef.platform.configuration.domain.Environment;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

/**
 * Maps integrations, their instance configurations and workflows, app events and published automation projects to their
 * proto messages and back through {@link ProtoJsonMapper}, leaving out the properties derived from others.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedConfigurationProtoMapper {

    private final PlatformConfigurationProtoMapper platformConfigurationProtoMapper;
    private final ProtoJsonMapper protoJsonMapper;

    public EmbeddedConfigurationProtoMapper(ObjectMapper objectMapper) {
        this.platformConfigurationProtoMapper = new PlatformConfigurationProtoMapper(objectMapper);
        this.protoJsonMapper = new ProtoJsonMapper(
            objectMapper.rebuild()
                .addMixIn(Integration.class, IntegrationMixIn.class)
                .addMixIn(IntegrationInstanceConfiguration.class, IntegrationInstanceConfigurationMixIn.class)
                .addMixIn(
                    IntegrationInstanceConfigurationWorkflow.class, IntegrationInstanceConfigurationWorkflowMixIn.class)
                .addMixIn(
                    IntegrationInstanceConfigurationWorkflowConnection.class,
                    IntegrationInstanceConfigurationWorkflowConnectionMixIn.class)
                .addMixIn(IntegrationInstance.class, NoArgsCreatorMixIn.class)
                .addMixIn(IntegrationInstanceWorkflow.class, NoArgsCreatorMixIn.class)
                .addMixIn(IntegrationVersion.class, NoArgsCreatorMixIn.class)
                .addMixIn(IntegrationWorkflow.class, IntegrationWorkflowMixIn.class)
                .build());
    }

    public AppEvent toAppEvent(RemoteAppEvent remoteAppEvent) {
        return protoJsonMapper.toObject(remoteAppEvent, AppEvent.class);
    }

    public List<AutomationWorkflowProjectDTO> toAutomationWorkflowProjectDTOs(
        RemoteAutomationWorkflowProjects remoteAutomationWorkflowProjects) {

        return remoteAutomationWorkflowProjects.getProjectsList()
            .stream()
            .map(remoteAutomationWorkflowProject -> protoJsonMapper.toObject(
                remoteAutomationWorkflowProject, AutomationWorkflowProjectDTO.class))
            .toList();
    }

    public static Environment toEnvironment(int ordinal) {
        Environment[] environments = Environment.values();

        if (ordinal < 0 || ordinal >= environments.length) {
            throw new IllegalArgumentException("Unknown environment: " + ordinal);
        }

        return environments[ordinal];
    }

    public Integration toIntegration(RemoteIntegration remoteIntegration) {
        return protoJsonMapper.toObject(remoteIntegration, Integration.class);
    }

    public IntegrationInstanceConfiguration toIntegrationInstanceConfiguration(
        RemoteIntegrationInstanceConfiguration remoteIntegrationInstanceConfiguration) {

        return protoJsonMapper.toObject(remoteIntegrationInstanceConfiguration, IntegrationInstanceConfiguration.class);
    }

    public List<IntegrationInstanceConfiguration> toIntegrationInstanceConfigurations(
        RemoteIntegrationInstanceConfigurations remoteIntegrationInstanceConfigurations) {

        return remoteIntegrationInstanceConfigurations.getIntegrationInstanceConfigurationsList()
            .stream()
            .map(this::toIntegrationInstanceConfiguration)
            .toList();
    }

    public IntegrationInstanceConfigurationWorkflow toIntegrationInstanceConfigurationWorkflow(
        RemoteIntegrationInstanceConfigurationWorkflow remoteIntegrationInstanceConfigurationWorkflow) {

        return protoJsonMapper.toObject(
            remoteIntegrationInstanceConfigurationWorkflow, IntegrationInstanceConfigurationWorkflow.class);
    }

    public List<IntegrationInstanceConfigurationWorkflow> toIntegrationInstanceConfigurationWorkflows(
        RemoteIntegrationInstanceConfigurationWorkflows remoteIntegrationInstanceConfigurationWorkflows) {

        return remoteIntegrationInstanceConfigurationWorkflows.getIntegrationInstanceConfigurationWorkflowsList()
            .stream()
            .map(this::toIntegrationInstanceConfigurationWorkflow)
            .toList();
    }

    public IntegrationInstanceConfigurationWorkflowConnection toIntegrationInstanceConfigurationWorkflowConnection(
        RemoteIntegrationInstanceConfigurationWorkflowConnection remoteConnection) {

        return protoJsonMapper.toObject(remoteConnection, IntegrationInstanceConfigurationWorkflowConnection.class);
    }

    public List<IntegrationInstanceConfigurationWorkflowConnection>
        toIntegrationInstanceConfigurationWorkflowConnections(
            RemoteIntegrationInstanceConfigurationWorkflowConnections remoteConnections) {

        return remoteConnections.getConnectionsList()
            .stream()
            .map(this::toIntegrationInstanceConfigurationWorkflowConnection)
            .toList();
    }

    public IntegrationInstance toIntegrationInstance(RemoteIntegrationInstance remoteIntegrationInstance) {
        return protoJsonMapper.toObject(remoteIntegrationInstance, IntegrationInstance.class);
    }

    public List<IntegrationInstance> toIntegrationInstances(RemoteIntegrationInstances remoteIntegrationInstances) {
        return remoteIntegrationInstances.getIntegrationInstancesList()
            .stream()
            .map(this::toIntegrationInstance)
            .toList();
    }

    public IntegrationInstanceWorkflow toIntegrationInstanceWorkflow(
        RemoteIntegrationInstanceWorkflow remoteIntegrationInstanceWorkflow) {

        return protoJsonMapper.toObject(remoteIntegrationInstanceWorkflow, IntegrationInstanceWorkflow.class);
    }

    public List<IntegrationInstanceWorkflow> toIntegrationInstanceWorkflows(
        RemoteIntegrationInstanceWorkflows remoteIntegrationInstanceWorkflows) {

        return remoteIntegrationInstanceWorkflows.getIntegrationInstanceWorkflowsList()
            .stream()
            .map(this::toIntegrationInstanceWorkflow)
            .toList();
    }

    public IntegrationWorkflow toIntegrationWorkflow(RemoteIntegrationWorkflow remoteIntegrationWorkflow) {
        return protoJsonMapper.toObject(remoteIntegrationWorkflow, IntegrationWorkflow.class);
    }

    public IntegrationWorkflowDTO toIntegrationWorkflowDTO(RemoteIntegrationWorkflowDTO remoteIntegrationWorkflowDTO) {
        IntegrationWorkflow integrationWorkflow = new IntegrationWorkflow(
            remoteIntegrationWorkflowDTO.getIntegrationWorkflowId());

        integrationWorkflow.setIntegrationVersion(remoteIntegrationWorkflowDTO.getIntegrationVersion());

        if (remoteIntegrationWorkflowDTO.hasWorkflowUuid()) {
            integrationWorkflow.setUuid(remoteIntegrationWorkflowDTO.getWorkflowUuid());
        }

        Workflow workflow = platformConfigurationProtoMapper.toWorkflow(remoteIntegrationWorkflowDTO.getWorkflow());

        return new IntegrationWorkflowDTO(workflow, integrationWorkflow);
    }

    public List<IntegrationWorkflowDTO> toIntegrationWorkflowDTOs(
        RemoteIntegrationWorkflowDTOs remoteIntegrationWorkflowDTOs) {

        return remoteIntegrationWorkflowDTOs.getIntegrationWorkflowsList()
            .stream()
            .map(this::toIntegrationWorkflowDTO)
            .toList();
    }

    public List<IntegrationWorkflow> toIntegrationWorkflows(RemoteIntegrationWorkflows remoteIntegrationWorkflows) {
        return remoteIntegrationWorkflows.getIntegrationWorkflowsList()
            .stream()
            .map(this::toIntegrationWorkflow)
            .toList();
    }

    public List<Integration> toIntegrations(RemoteIntegrations remoteIntegrations) {
        return remoteIntegrations.getIntegrationsList()
            .stream()
            .map(this::toIntegration)
            .toList();
    }

    public RemoteAppEvent toProto(AppEvent appEvent) {
        return protoJsonMapper.toProto(appEvent, RemoteAppEvent.newBuilder());
    }

    public RemoteIntegration toProto(Integration integration) {
        return protoJsonMapper.toProto(integration, RemoteIntegration.newBuilder());
    }

    public RemoteIntegrationInstanceConfiguration toProto(
        IntegrationInstanceConfiguration integrationInstanceConfiguration) {

        return protoJsonMapper.toProto(
            integrationInstanceConfiguration, RemoteIntegrationInstanceConfiguration.newBuilder());
    }

    public RemoteIntegrationInstanceConfigurationWorkflow toProto(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        return protoJsonMapper.toProto(
            integrationInstanceConfigurationWorkflow, RemoteIntegrationInstanceConfigurationWorkflow.newBuilder());
    }

    public RemoteIntegrationInstanceConfigurationWorkflowConnection toProto(
        IntegrationInstanceConfigurationWorkflowConnection integrationInstanceConfigurationWorkflowConnection) {

        return protoJsonMapper.toProto(
            integrationInstanceConfigurationWorkflowConnection,
            RemoteIntegrationInstanceConfigurationWorkflowConnection.newBuilder());
    }

    public RemoteIntegrationInstance toProto(IntegrationInstance integrationInstance) {
        return protoJsonMapper.toProto(integrationInstance, RemoteIntegrationInstance.newBuilder());
    }

    public RemoteIntegrationInstances toProto(List<IntegrationInstance> integrationInstances) {
        return RemoteIntegrationInstances.newBuilder()
            .addAllIntegrationInstances(
                integrationInstances.stream()
                    .map(this::toProto)
                    .toList())
            .build();
    }

    public RemoteIntegrationInstanceWorkflow toProto(IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        return protoJsonMapper.toProto(integrationInstanceWorkflow, RemoteIntegrationInstanceWorkflow.newBuilder());
    }

    public RemoteIntegrationWorkflow toProto(IntegrationWorkflow integrationWorkflow) {
        return protoJsonMapper.toProto(integrationWorkflow, RemoteIntegrationWorkflow.newBuilder());
    }

    public RemoteIntegrationWorkflowDTO toProto(IntegrationWorkflowDTO integrationWorkflowDTO) {
        RemoteIntegrationWorkflowDTO.Builder builder = RemoteIntegrationWorkflowDTO.newBuilder()
            .setIntegrationVersion(integrationWorkflowDTO.getIntegrationVersion())
            .setIntegrationWorkflowId(integrationWorkflowDTO.getIntegrationWorkflowId())
            .setWorkflow(platformConfigurationProtoMapper.toProto(integrationWorkflowDTO.getWorkflow()));

        String workflowUuid = integrationWorkflowDTO.getWorkflowUuid();

        if (workflowUuid != null) {
            builder.setWorkflowUuid(workflowUuid);
        }

        return builder.build();
    }

    public RemoteAutomationWorkflowProjects toRemoteAutomationWorkflowProjects(
        List<AutomationWorkflowProjectDTO> automationWorkflowProjectDTOs) {

        RemoteAutomationWorkflowProjects.Builder builder = RemoteAutomationWorkflowProjects.newBuilder();

        for (AutomationWorkflowProjectDTO automationWorkflowProjectDTO : automationWorkflowProjectDTOs) {
            builder.addProjects(
                protoJsonMapper.<RemoteAutomationWorkflowProject>toProto(
                    automationWorkflowProjectDTO, RemoteAutomationWorkflowProject.newBuilder()));
        }

        return builder.build();
    }

    public RemoteIntegrationInstanceConfigurationWorkflowConnections
        toRemoteIntegrationInstanceConfigurationWorkflowConnections(
            List<IntegrationInstanceConfigurationWorkflowConnection> integrationInstanceConfigurationWorkflowConnections) {

        RemoteIntegrationInstanceConfigurationWorkflowConnections.Builder builder =
            RemoteIntegrationInstanceConfigurationWorkflowConnections.newBuilder();

        for (IntegrationInstanceConfigurationWorkflowConnection integrationInstanceConfigurationWorkflowConnection : integrationInstanceConfigurationWorkflowConnections) {

            builder.addConnections(toProto(integrationInstanceConfigurationWorkflowConnection));
        }

        return builder.build();
    }

    public RemoteIntegrationInstanceConfigurationWorkflows toRemoteIntegrationInstanceConfigurationWorkflows(
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows) {

        return RemoteIntegrationInstanceConfigurationWorkflows.newBuilder()
            .addAllIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfigurationWorkflows.stream()
                    .map(this::toProto)
                    .toList())
            .build();
    }

    public RemoteIntegrationInstanceConfigurations toRemoteIntegrationInstanceConfigurations(
        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations) {

        return RemoteIntegrationInstanceConfigurations.newBuilder()
            .addAllIntegrationInstanceConfigurations(
                integrationInstanceConfigurations.stream()
                    .map(this::toProto)
                    .toList())
            .build();
    }

    public RemoteIntegrationInstanceWorkflows toRemoteIntegrationInstanceWorkflows(
        List<IntegrationInstanceWorkflow> integrationInstanceWorkflows) {

        return RemoteIntegrationInstanceWorkflows.newBuilder()
            .addAllIntegrationInstanceWorkflows(
                integrationInstanceWorkflows.stream()
                    .map(this::toProto)
                    .toList())
            .build();
    }

    public RemoteIntegrationWorkflowDTOs toRemoteIntegrationWorkflowDTOs(
        List<IntegrationWorkflowDTO> integrationWorkflowDTOs) {

        return RemoteIntegrationWorkflowDTOs.newBuilder()
            .addAllIntegrationWorkflows(
                integrationWorkflowDTOs.stream()
                    .map(this::toProto)
                    .toList())
            .build();
    }

    public RemoteIntegrationWorkflows toRemoteIntegrationWorkflows(List<IntegrationWorkflow> integrationWorkflows) {
        return RemoteIntegrationWorkflows.newBuilder()
            .addAllIntegrationWorkflows(
                integrationWorkflows.stream()
                    .map(this::toProto)
                    .toList())
            .build();
    }

    public RemoteIntegrations toRemoteIntegrations(List<Integration> integrations) {
        RemoteIntegrations.Builder builder = RemoteIntegrations.newBuilder();

        for (Integration integration : integrations) {
            builder.addIntegrations(toProto(integration));
        }

        return builder.build();
    }

    @JsonIgnoreProperties({
        "lastIntegrationVersion", "lastPublishedDate", "lastStatus", "published"
    })
    private abstract static class IntegrationMixIn {

        @JsonCreator
        IntegrationMixIn() {
        }
    }

    @JsonIgnoreProperties("environmentId")
    private abstract static class IntegrationInstanceConfigurationMixIn {
    }

    @JsonIgnoreProperties("connectionsCount")
    private abstract static class IntegrationInstanceConfigurationWorkflowMixIn {

        @JsonCreator
        IntegrationInstanceConfigurationWorkflowMixIn() {
        }
    }

    private abstract static class IntegrationInstanceConfigurationWorkflowConnectionMixIn {

        @JsonCreator
        @SuppressWarnings("PMD.UnusedFormalParameter")
        IntegrationInstanceConfigurationWorkflowConnectionMixIn(
            @JsonProperty("connectionId") Long connectionId,
            @JsonProperty("workflowConnectionKey") String workflowConnectionKey,
            @JsonProperty("workflowNodeName") String workflowNodeName) {
        }
    }

    @JsonIgnoreProperties("uuidAsString")
    private abstract static class IntegrationWorkflowMixIn {

        @JsonCreator
        IntegrationWorkflowMixIn() {
        }

        @JsonProperty("uuid")
        abstract void setUuid(String uuid);
    }

    private abstract static class NoArgsCreatorMixIn {

        @JsonCreator
        NoArgsCreatorMixIn() {
        }
    }
}
