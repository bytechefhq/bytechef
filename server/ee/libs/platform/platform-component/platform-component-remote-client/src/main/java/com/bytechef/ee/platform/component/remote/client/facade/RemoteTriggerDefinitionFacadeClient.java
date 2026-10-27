/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.component.remote.client.WorkerInstances;
import com.bytechef.ee.platform.component.remote.grpc.ComponentProtoMapper;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerDefinitionFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerDefinitionFacadeRemoteServiceGrpc.TriggerDefinitionFacadeRemoteServiceBlockingStub;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteDynamicPropertiesRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteDynamicWebhookRefreshRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteListenerRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteOptionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteOutputRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteTriggerRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteWebhookDisableRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteWebhookEnableRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteWebhookValidateRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTriggerDefinitionFacadeClient implements TriggerDefinitionFacade {

    private final ComponentProtoMapper componentProtoMapper;
    private final Duration executionDeadline;
    private final GrpcChannels grpcChannels;
    private final WorkerInstances workerInstances;

    public RemoteTriggerDefinitionFacadeClient(
        ApplicationProperties applicationProperties, GrpcChannels grpcChannels, DiscoveryClient discoveryClient,
        ObjectMapper objectMapper) {

        ApplicationProperties.Remote remote = applicationProperties.getRemote();
        ApplicationProperties.Remote.Grpc grpc = remote.getGrpc();

        this.componentProtoMapper = new ComponentProtoMapper(objectMapper);
        this.executionDeadline = grpc.getExecutionDeadline();
        this.grpcChannels = grpcChannels;
        this.workerInstances = new WorkerInstances(discoveryClient);
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, Long connectionId) {

        TriggerExecuteDynamicPropertiesRequest.Builder builder = TriggerExecuteDynamicPropertiesRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setTriggerName(triggerName)
            .setPropertyName(propertyName)
            .setInputParameters(componentProtoMapper.toBytes(inputParameters))
            .addAllLookupDependsOnPaths(lookupDependsOnPaths == null ? List.of() : lookupDependsOnPaths);

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        return componentProtoMapper.toProperties(getStub(componentName).executeDynamicProperties(builder.build()));
    }

    @Override
    public WebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, Map<String, ?> outputParameters,
        Long connectionId) {

        TriggerExecuteDynamicWebhookRefreshRequest.Builder builder =
            TriggerExecuteDynamicWebhookRefreshRequest.newBuilder()
                .setComponentName(componentName)
                .setComponentVersion(componentVersion)
                .setTriggerName(triggerName)
                .setOutputParameters(componentProtoMapper.toBytes(outputParameters));

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        return componentProtoMapper.toWebhookEnableOutput(
            getStub(componentName).executeDynamicWebhookRefresh(builder.build()));
    }

    @Override
    public void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Long connectionId) {

        getStub(componentName).executeListenerDisable(
            getListenerRequest(
                componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, connectionId));
    }

    @Override
    public void executeListenerEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Long connectionId) {

        getStub(componentName).executeListenerEnable(
            getListenerRequest(
                componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, connectionId));
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText, Long connectionId) {

        TriggerExecuteOptionsRequest.Builder builder = TriggerExecuteOptionsRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setTriggerName(triggerName)
            .setPropertyName(propertyName)
            .setInputParameters(componentProtoMapper.toBytes(inputParameters))
            .addAllLookupDependsOnPaths(lookupDependsOnPaths == null ? List.of() : lookupDependsOnPaths);

        if (searchText != null) {
            builder.setSearchText(searchText);
        }

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        return componentProtoMapper.toOptions(getStub(componentName).executeOptions(builder.build()));
    }

    @Override
    public OutputResponse executeOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        Long connectionId) {

        TriggerExecuteOutputRequest.Builder builder = TriggerExecuteOutputRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setTriggerName(triggerName)
            .setInputParameters(componentProtoMapper.toBytes(inputParameters));

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        return componentProtoMapper.toOutputResponse(getStub(componentName).executeOutput(builder.build()));
    }

    @Override
    public TriggerOutput executeTrigger(
        String componentName, int componentVersion, String triggerName, Long jobPrincipalId, String workflowUuid,
        Long triggerExecutionId, Map<String, ?> inputParameters, Object triggerState, WebhookRequest webhookRequest,
        Long connectionId, Long environmentId, PlatformType type, boolean editorEnvironment) {

        TriggerExecuteTriggerRequest.Builder builder = TriggerExecuteTriggerRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setTriggerName(triggerName)
            .setInputParameters(componentProtoMapper.toBytes(inputParameters))
            .setEditorEnvironment(editorEnvironment);

        if (jobPrincipalId != null) {
            builder.setJobPrincipalId(jobPrincipalId);
        }

        if (workflowUuid != null) {
            builder.setWorkflowUuid(workflowUuid);
        }

        if (triggerExecutionId != null) {
            builder.setTriggerExecutionId(triggerExecutionId);
        }

        if (triggerState != null) {
            builder.setTriggerState(componentProtoMapper.toBytes(triggerState));
        }

        if (webhookRequest != null) {
            builder.setWebhookRequest(componentProtoMapper.toProto(webhookRequest));
        }

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        if (environmentId != null) {
            builder.setEnvironmentId(environmentId);
        }

        if (type != null) {
            builder.setPlatformType(type.name());
        }

        return componentProtoMapper.toTriggerOutput(getStub(componentName).executeTrigger(builder.build()));
    }

    @Override
    public void executeWebhookDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Map<String, ?> outputParameters, Long connectionId) {

        TriggerExecuteWebhookDisableRequest.Builder builder = TriggerExecuteWebhookDisableRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setTriggerName(triggerName)
            .setInputParameters(componentProtoMapper.toBytes(inputParameters))
            .setOutputParameters(componentProtoMapper.toBytes(outputParameters));

        if (workflowExecutionId != null) {
            builder.setWorkflowExecutionId(workflowExecutionId);
        }

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        getStub(componentName).executeWebhookDisable(builder.build());
    }

    @Override
    public WebhookEnableOutput executeWebhookEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Long connectionId, String webhookUrl, long environmentId) {

        TriggerExecuteWebhookEnableRequest.Builder builder = TriggerExecuteWebhookEnableRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setTriggerName(triggerName)
            .setInputParameters(componentProtoMapper.toBytes(inputParameters))
            .setEnvironmentId(environmentId);

        if (workflowExecutionId != null) {
            builder.setWorkflowExecutionId(workflowExecutionId);
        }

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        if (webhookUrl != null) {
            builder.setWebhookUrl(webhookUrl);
        }

        return componentProtoMapper.toWebhookEnableOutput(getStub(componentName).executeWebhookEnable(builder.build()));
    }

    @Override
    public WebhookValidateResponse executeWebhookValidate(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, Long connectionId) {

        return componentProtoMapper.toWebhookValidateResponse(
            getStub(componentName).executeWebhookValidate(
                getWebhookValidateRequest(
                    componentName, componentVersion, triggerName, inputParameters, webhookRequest, connectionId)));
    }

    @Override
    public WebhookValidateResponse executeWebhookValidateOnEnable(
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, WebhookRequest webhookRequest, Long connectionId) {

        return componentProtoMapper.toWebhookValidateResponse(
            getStub(componentName).executeWebhookValidateOnEnable(
                getWebhookValidateRequest(
                    componentName, componentVersion, triggerName, inputParameters, webhookRequest, connectionId)));
    }

    private TriggerExecuteListenerRequest getListenerRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Long connectionId) {

        TriggerExecuteListenerRequest.Builder builder = TriggerExecuteListenerRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setTriggerName(triggerName)
            .setInputParameters(componentProtoMapper.toBytes(inputParameters));

        if (workflowExecutionId != null) {
            builder.setWorkflowExecutionId(workflowExecutionId);
        }

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        return builder.build();
    }

    private TriggerExecuteWebhookValidateRequest getWebhookValidateRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, Long connectionId) {

        TriggerExecuteWebhookValidateRequest.Builder builder = TriggerExecuteWebhookValidateRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setTriggerName(triggerName)
            .setInputParameters(componentProtoMapper.toBytes(inputParameters));

        if (webhookRequest != null) {
            builder.setWebhookRequest(componentProtoMapper.toProto(webhookRequest));
        }

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        return builder.build();
    }

    private TriggerDefinitionFacadeRemoteServiceBlockingStub getStub(String componentName) {
        TriggerDefinitionFacadeRemoteServiceBlockingStub stub =
            TriggerDefinitionFacadeRemoteServiceGrpc.newBlockingStub(
                grpcChannels.getChannel(workerInstances.getInstance(componentName)));

        if (executionDeadline == null) {
            return stub;
        }

        return stub.withDeadlineAfter(executionDeadline.toMillis(), TimeUnit.MILLISECONDS);
    }
}
