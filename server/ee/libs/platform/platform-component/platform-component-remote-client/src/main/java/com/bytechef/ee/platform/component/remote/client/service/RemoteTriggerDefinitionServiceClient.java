/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.ee.platform.component.remote.client.WorkerInstances;
import com.bytechef.ee.platform.component.remote.grpc.ComponentProtoMapper;
import com.bytechef.ee.platform.component.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.platform.component.remote.grpc.proto.ComponentVersionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ExecuteWorkflowNodeDescriptionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetTriggerDefinitionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteTriggerDefinitions;
import com.bytechef.ee.platform.component.remote.grpc.proto.StringResponse;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerDefinitionRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerDefinitionRemoteServiceGrpc.TriggerDefinitionRemoteServiceBlockingStub;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.ee.remote.grpc.ProtoValues;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
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
public class RemoteTriggerDefinitionServiceClient implements TriggerDefinitionService {

    private final ComponentProtoMapper componentProtoMapper;
    private final GrpcChannels grpcChannels;
    private final WorkerInstances workerInstances;

    public RemoteTriggerDefinitionServiceClient(
        GrpcChannels grpcChannels, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        this.componentProtoMapper = new ComponentProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
        this.workerInstances = new WorkerInstances(discoveryClient);
    }

    @Override
    public void executeWebhookDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Map<String, ?> outputParameters, ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String propertyName, List<String> lookupDependsOnPaths, ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public WebhookEnableOutput executeWebhookEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String webhookUrl, String workflowExecutionId, ComponentConnection componentConnection, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public WebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, ComponentConnection componentConnection,
        Map<String, ?> outputParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeListenerEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public OutputResponse executeOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public WebhookValidateResponse executeWebhookValidate(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public WebhookValidateResponse executeWebhookValidateOnEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters) {

        ExecuteWorkflowNodeDescriptionRequest executeWorkflowNodeDescriptionRequest =
            ExecuteWorkflowNodeDescriptionRequest.newBuilder()
                .setComponentName(componentName)
                .setComponentVersion(componentVersion)
                .setTriggerName(triggerName)
                .setInputParameters(ProtoValues.toStruct(inputParameters))
                .build();

        StringResponse stringResponse = getStub(componentName).executeWorkflowNodeDescription(
            executeWorkflowNodeDescriptionRequest);

        return stringResponse.hasValue() ? stringResponse.getValue() : null;
    }

    @Override
    public List<String> getPropertyLookupDependsOn(
        String componentName, int componentVersion, String triggerName, String propertyName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerDefinition getTriggerDefinition(String componentName, int componentVersion, String triggerName) {
        return componentProtoMapper.toTriggerDefinition(
            getStub(componentName).getTriggerDefinition(
                getTriggerDefinitionRequest(componentName, componentVersion, triggerName)));
    }

    @Override
    public List<TriggerDefinition> getTriggerDefinitions(String componentName, int componentVersion) {
        ComponentVersionRequest componentVersionRequest = ComponentVersionRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .build();

        RemoteTriggerDefinitions remoteTriggerDefinitions = getStub(componentName).getTriggerDefinitions(
            componentVersionRequest);

        return remoteTriggerDefinitions.getTriggerDefinitionsList()
            .stream()
            .map(componentProtoMapper::toTriggerDefinition)
            .toList();
    }

    @Override
    public WebhookTriggerFlags getWebhookTriggerFlags(String componentName, int componentVersion, String triggerName) {
        return componentProtoMapper.toWebhookTriggerFlags(
            getStub(componentName).getWebhookTriggerFlags(
                getTriggerDefinitionRequest(componentName, componentVersion, triggerName)));
    }

    @Override
    public boolean isDynamicOutputDefined(String componentName, int componentVersion, String triggerName) {
        BoolResponse boolResponse = getStub(componentName).isDynamicOutputDefined(
            getTriggerDefinitionRequest(componentName, componentVersion, triggerName));

        return boolResponse.getValue();
    }

    @Override
    public boolean propertyHasOptionsDataSource(
        String componentName, int componentVersion, String triggerName, String propertyName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean triggerDefinesConnection(String componentName, int componentVersion, String triggerName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, int connectionVersion, String componentOperationName,
        int statusCode, Object body, Map<String, List<String>> headers) {

        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerOutput executeTrigger(
        String componentName, int componentVersion, String triggerName, Long jobPrincipalId, String workflowUuid,
        Long triggerExecutionId, Map<String, ?> inputParameters, Object triggerState, WebhookRequest webhookRequest,
        ComponentConnection componentConnection, Long environmentId, PlatformType type, boolean editorEnvironment) {

        throw new UnsupportedOperationException();
    }

    private TriggerDefinitionRemoteServiceBlockingStub getStub(String componentName) {
        return TriggerDefinitionRemoteServiceGrpc.newBlockingStub(
            grpcChannels.getChannel(workerInstances.getInstance(componentName)));
    }

    private static GetTriggerDefinitionRequest getTriggerDefinitionRequest(
        String componentName, int componentVersion, String triggerName) {

        return GetTriggerDefinitionRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setTriggerName(triggerName)
            .build();
    }
}
