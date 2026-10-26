/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.ee.platform.component.remote.client.WorkerInstances;
import com.bytechef.ee.platform.component.remote.grpc.ComponentProtoMapper;
import com.bytechef.ee.platform.component.remote.grpc.proto.ActionDefinitionRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.ActionDefinitionRemoteServiceGrpc.ActionDefinitionRemoteServiceBlockingStub;
import com.bytechef.ee.platform.component.remote.grpc.proto.ActionExecuteWorkflowNodeDescriptionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.platform.component.remote.grpc.proto.ComponentVersionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetActionDefinitionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteActionDefinitions;
import com.bytechef.ee.platform.component.remote.grpc.proto.StringResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteActionDefinitionServiceClient implements ActionDefinitionService {

    private final ComponentProtoMapper componentProtoMapper;
    private final GrpcChannels grpcChannels;
    private final WorkerInstances workerInstances;

    public RemoteActionDefinitionServiceClient(
        GrpcChannels grpcChannels, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        this.componentProtoMapper = new ComponentProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
        this.workerInstances = new WorkerInstances(discoveryClient);
    }

    @Override
    public boolean actionDefinesConnection(String componentName, int componentVersion, String actionName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String workflowId,
        @Nullable ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        Map<String, ComponentConnection> componentConnections, Map<String, ?> extensions) {

        throw new UnsupportedOperationException();
    }

    @Override
    public OutputResponse executeOutput(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Map<String, ComponentConnection> componentConnections) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executePerform(
        String componentName, int componentVersion, String actionName, Long jobPrincipalId, Long jobPrincipalWorkflowId,
        Long jobId, @Nullable Long taskExecutionId, String workflowId, Map<String, ?> inputParameters,
        Map<String, ComponentConnection> componentConnections, Map<String, ?> extensions, Long environmentId,
        boolean editorEnvironment, PlatformType type, Map<String, ?> continueParameters,
        Map<String, ?> resumeData, Instant suspendExpiresAt) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executePerformForPolyglot(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        ComponentConnection componentConnection, Long environmentId, ActionContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, int connectionVersion, String componentOperationName,
        int statusCode, Object body, Map<String, List<String>> headers) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters) {

        ActionExecuteWorkflowNodeDescriptionRequest actionExecuteWorkflowNodeDescriptionRequest =
            ActionExecuteWorkflowNodeDescriptionRequest.newBuilder()
                .setComponentName(componentName)
                .setComponentVersion(componentVersion)
                .setActionName(actionName)
                .setInputParameters(componentProtoMapper.toBytes(inputParameters))
                .build();

        StringResponse stringResponse = getStub(componentName).executeWorkflowNodeDescription(
            actionExecuteWorkflowNodeDescriptionRequest);

        return stringResponse.hasValue() ? stringResponse.getValue() : null;
    }

    @Override
    public ActionDefinition getActionDefinition(String componentName, int componentVersion, String actionName) {
        GetActionDefinitionRequest getActionDefinitionRequest = GetActionDefinitionRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setActionName(actionName)
            .build();

        return componentProtoMapper.toActionDefinition(
            getStub(componentName).getActionDefinition(getActionDefinitionRequest));
    }

    @Override
    public List<ActionDefinition> getActionDefinitions(String componentName, int componentVersion) {
        ComponentVersionRequest componentVersionRequest = ComponentVersionRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .build();

        RemoteActionDefinitions remoteActionDefinitions = getStub(componentName).getActionDefinitions(
            componentVersionRequest);

        return remoteActionDefinitions.getActionDefinitionsList()
            .stream()
            .map(componentProtoMapper::toActionDefinition)
            .toList();
    }

    @Override
    public List<String> getPropertyLookupDependsOn(
        String componentName, int componentVersion, String actionName, String propertyName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDynamicOutputDefined(String componentName, int componentVersion, String actionName) {
        GetActionDefinitionRequest getActionDefinitionRequest = GetActionDefinitionRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setActionName(actionName)
            .build();

        BoolResponse boolResponse = getStub(componentName).isDynamicOutputDefined(getActionDefinitionRequest);

        return boolResponse.getValue();
    }

    @Override
    public boolean propertyHasOptionsDataSource(
        String componentName, int componentVersion, String actionName, String propertyName) {

        throw new UnsupportedOperationException();
    }

    private ActionDefinitionRemoteServiceBlockingStub getStub(String componentName) {
        return ActionDefinitionRemoteServiceGrpc.newBlockingStub(
            grpcChannels.getChannel(workerInstances.getInstance(componentName)));
    }
}
