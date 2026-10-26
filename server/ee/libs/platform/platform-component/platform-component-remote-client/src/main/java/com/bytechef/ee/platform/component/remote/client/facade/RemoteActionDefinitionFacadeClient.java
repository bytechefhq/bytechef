/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import com.bytechef.ee.platform.component.remote.client.WorkerInstances;
import com.bytechef.ee.platform.component.remote.grpc.ComponentProtoMapper;
import com.bytechef.ee.platform.component.remote.grpc.proto.ActionDefinitionFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.ActionDefinitionFacadeRemoteServiceGrpc.ActionDefinitionFacadeRemoteServiceBlockingStub;
import com.bytechef.ee.platform.component.remote.grpc.proto.ActionExecuteDynamicPropertiesRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ActionExecuteOptionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ActionExecuteOutputRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ActionExecutePerformRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ValueResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.ee.remote.grpc.ProtoValues;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteActionDefinitionFacadeClient implements ActionDefinitionFacade {

    private final ComponentProtoMapper componentProtoMapper;
    private final GrpcChannels grpcChannels;
    private final WorkerInstances workerInstances;

    public RemoteActionDefinitionFacadeClient(
        GrpcChannels grpcChannels, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        this.componentProtoMapper = new ComponentProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
        this.workerInstances = new WorkerInstances(discoveryClient);
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String workflowId, Long connectionId) {

        ActionExecuteDynamicPropertiesRequest.Builder builder = ActionExecuteDynamicPropertiesRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setActionName(actionName)
            .setPropertyName(propertyName)
            .setInputParameters(ProtoValues.toStruct(inputParameters))
            .addAllLookupDependsOnPaths(lookupDependsOnPaths == null ? List.of() : lookupDependsOnPaths);

        if (workflowId != null) {
            builder.setWorkflowId(workflowId);
        }

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        return componentProtoMapper.toProperties(getStub(componentName).executeDynamicProperties(builder.build()));
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText, Long connectionId) {

        ActionExecuteOptionsRequest.Builder builder = getExecuteOptionsRequestBuilder(
            componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
            searchText);

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        return componentProtoMapper.toOptions(getStub(componentName).executeOptions(builder.build()));
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        Map<String, Long> connectionIds, Map<String, ?> extensions) {

        ActionExecuteOptionsRequest.Builder builder = getExecuteOptionsRequestBuilder(
            componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
            searchText)
                .setMultipleConnections(true);

        if (connectionIds != null) {
            builder.putAllConnectionIds(connectionIds);
        }

        if (extensions != null) {
            builder.setExtensions(ProtoValues.toStruct(extensions));
        }

        return componentProtoMapper.toOptions(getStub(componentName).executeOptions(builder.build()));
    }

    @Override
    public OutputResponse executeOutput(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Map<String, Long> connectionIds) {

        ActionExecuteOutputRequest.Builder builder = ActionExecuteOutputRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setActionName(actionName)
            .setInputParameters(ProtoValues.toStruct(inputParameters));

        if (connectionIds != null) {
            builder.putAllConnectionIds(connectionIds);
        }

        return componentProtoMapper.toOutputResponse(getStub(componentName).executeOutput(builder.build()));
    }

    @Override
    public Object executePerform(
        String componentName, int componentVersion, String actionName, Long jobPrincipalId,
        Long jobPrincipalWorkflowId, Long jobId, Long taskExecutionId, String workflowId,
        Map<String, ?> inputParameters, Map<String, Long> connectionIds, Map<String, ?> extensions,
        Long environmentId, PlatformType type, boolean editorEnvironment, Map<String, ?> continueParameters,
        Map<String, ?> resumeData, Instant suspendExpiresAt) {

        ActionExecutePerformRequest.Builder builder = ActionExecutePerformRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setActionName(actionName)
            .setInputParameters(ProtoValues.toStruct(inputParameters))
            .setEditorEnvironment(editorEnvironment);

        if (jobPrincipalId != null) {
            builder.setJobPrincipalId(jobPrincipalId);
        }

        if (jobPrincipalWorkflowId != null) {
            builder.setJobPrincipalWorkflowId(jobPrincipalWorkflowId);
        }

        if (jobId != null) {
            builder.setJobId(jobId);
        }

        if (taskExecutionId != null) {
            builder.setTaskExecutionId(taskExecutionId);
        }

        if (workflowId != null) {
            builder.setWorkflowId(workflowId);
        }

        if (connectionIds != null) {
            builder.putAllConnectionIds(connectionIds);
        }

        if (extensions != null) {
            builder.setExtensions(ProtoValues.toStruct(extensions));
        }

        if (environmentId != null) {
            builder.setEnvironmentId(environmentId);
        }

        if (type != null) {
            builder.setPlatformType(type.name());
        }

        if (continueParameters != null) {
            builder.setContinueParameters(ProtoValues.toStruct(continueParameters));
        }

        if (resumeData != null) {
            builder.setResumeData(ProtoValues.toStruct(resumeData));
        }

        if (suspendExpiresAt != null) {
            builder.setSuspendExpiresAt(
                Timestamp.newBuilder()
                    .setSeconds(suspendExpiresAt.getEpochSecond())
                    .setNanos(suspendExpiresAt.getNano())
                    .build());
        }

        ValueResponse valueResponse = getStub(componentName).executePerform(builder.build());

        return ProtoValues.toObject(valueResponse.getValue());
    }

    private static ActionExecuteOptionsRequest.Builder getExecuteOptionsRequestBuilder(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText) {

        ActionExecuteOptionsRequest.Builder builder = ActionExecuteOptionsRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setActionName(actionName)
            .setPropertyName(propertyName)
            .setInputParameters(ProtoValues.toStruct(inputParameters))
            .addAllLookupDependsOnPaths(lookupDependsOnPaths == null ? List.of() : lookupDependsOnPaths);

        if (searchText != null) {
            builder.setSearchText(searchText);
        }

        return builder;
    }

    private ActionDefinitionFacadeRemoteServiceBlockingStub getStub(String componentName) {
        return ActionDefinitionFacadeRemoteServiceGrpc.newBlockingStub(
            grpcChannels.getChannel(workerInstances.getInstance(componentName)));
    }
}
