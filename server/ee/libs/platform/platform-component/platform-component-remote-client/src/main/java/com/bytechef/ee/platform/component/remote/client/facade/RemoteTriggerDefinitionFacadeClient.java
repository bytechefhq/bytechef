/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.ee.platform.component.remote.client.AbstractWorkerClient;
import com.bytechef.ee.remote.client.DefaultRestClient;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.registry.domain.OutputResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTriggerDefinitionFacadeClient extends AbstractWorkerClient implements TriggerDefinitionFacade {

    private static final String TRIGGER_DEFINITION_FACADE = "/trigger-definition-facade";

    public RemoteTriggerDefinitionFacadeClient(
        DefaultRestClient defaultRestClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultRestClient, discoveryClient, objectMapper);
    }

    @Override
    public List<Property> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-properties"),
            new PropertiesRequest(
                componentName, componentVersion, triggerName, propertyName, inputParameters, connectionId,
                lookupDependsOnPaths),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public WebhookEnableOutput executeDynamicWebhookRefresh(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> outputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-dynamic-webhook-refresh"),
            new DynamicWebhookRefresh(componentName, componentVersion, triggerName, outputParameters, connectionId),
            WebhookEnableOutput.class);
    }

    @Override
    public void executeListenerDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Long connectionId) {

        defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-listener-disable"),
            new ListenerDisableRequest(
                componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, connectionId));
    }

    @Override
    public void executeListenerEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Long connectionId) {

        defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-listener-enable"),
            new ListenerEnableRequest(
                componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, connectionId));
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, String searchText,
        Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-options"),
            new OptionsRequest(
                componentName, componentVersion, triggerName, propertyName, inputParameters, connectionId,
                lookupDependsOnPaths, searchText),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public OutputResponse executeOutput(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-output"),
            new OutputRequest(componentName, componentVersion, triggerName, inputParameters, connectionId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public TriggerOutput executeTrigger(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull AppType type,
        Long instanceId, String workflowReferenceCode, @NonNull Map<String, ?> inputParameters,
        Object triggerState, WebhookRequest webhookRequest, Long connectionId, boolean devEnvironment) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-trigger"),
            new TriggerRequest(
                componentName, componentVersion, triggerName, inputParameters, triggerState, webhookRequest,
                connectionId),
            TriggerOutput.class);
    }

    @Override
    public void executeWebhookDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId,
        @NonNull Map<String, ?> outputParameters,
        Long connectionId) {

        defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-webhook-disable"),
            new DynamicWebhookDisableRequest(
                componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, outputParameters,
                connectionId));
    }

    @Override
    public WebhookEnableOutput executeWebhookEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Long connectionId,
        @NonNull String webhookUrl) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-webhook-enable"),
            new DynamicWebhookEnableRequest(
                componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, connectionId,
                webhookUrl),
            WebhookEnableOutput.class);
    }

    @Override
    public WebhookValidateResponse executeWebhookValidate(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull WebhookRequest webhookRequest, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-webhook-validate"),
            new WebhookValidateRequest(
                componentName, componentVersion, triggerName, inputParameters, webhookRequest,
                connectionId),
            WebhookValidateResponse.class);
    }

    @Override
    public WebhookValidateResponse executeWebhookValidateOnEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull WebhookRequest webhookRequest, Long connectionId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String executeWorkflowNodeDescription(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-workflow-node-description"),
            new NodeDescriptionRequest(
                componentName, componentVersion, triggerName, inputParameters),
            String.class);
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, int statusCode, Object body) {
        throw new UnsupportedOperationException();
    }

    private record NodeDescriptionRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters) {
    }

    private record OptionsRequest(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, Long connectionId, List<String> lookupDependsOnPaths, String searchText) {
    }

    private record OutputRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        Long connectionId) {
    }

    private record PropertiesRequest(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, Long connectionId, List<String> loadDependsOnPath) {
    }

    private record SampleOutputRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        Long connectionId) {
    }

    private record DynamicWebhookDisableRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Map<String, ?> outputParameters, Long connectionIdd) {
    }

    private record DynamicWebhookEnableRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Long connectionId, String webhookUrl) {
    }

    private record DynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, Map<String, ?> outputParameters,
        Long connectionId) {
    }

    private record ListenerDisableRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Long connectionId) {
    }

    private record ListenerEnableRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Long connectionI) {
    }

    private record TriggerRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters, Object state,
        WebhookRequest webhookRequest, Long connectionId) {
    }

    private record WebhookValidateRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, Long connectionId) {
    }
}
