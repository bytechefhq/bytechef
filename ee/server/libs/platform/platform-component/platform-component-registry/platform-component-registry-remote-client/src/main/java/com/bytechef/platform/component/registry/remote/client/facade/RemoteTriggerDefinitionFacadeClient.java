/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.component.registry.remote.client.facade;

import com.bytechef.commons.rest.client.DefaultRestClient;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.registry.remote.client.AbstractWorkerClient;
import com.bytechef.platform.component.registry.trigger.TriggerOutput;
import com.bytechef.platform.component.registry.trigger.WebhookRequest;
import com.bytechef.platform.registry.domain.Option;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
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
@ConditionalOnMissingClass(value = "com.bytechef.worker.WorkerApplication")
public class RemoteTriggerDefinitionFacadeClient extends AbstractWorkerClient implements TriggerDefinitionFacade {

    private static final String TRIGGER_DEFINITION_FACADE = "/trigger-definition-facade";

    public RemoteTriggerDefinitionFacadeClient(
        DefaultRestClient defaultRestClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultRestClient, discoveryClient, objectMapper);
    }

    @Override
    public List<Property> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, Object> inputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-properties"),
            new PropertiesRequest(
                componentName, componentVersion, triggerName, propertyName, inputParameters, connectionId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public String executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-editor-description"),
            new EditorDescriptionRequest(
                componentName, componentVersion, triggerName, inputParameters, connectionId),
            String.class);
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
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> outputParameters) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-dynamic-webhook-refresh"),
            new DynamicWebhookRefresh(componentName, componentVersion, triggerName, outputParameters),
            DynamicWebhookEnableOutput.class);
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, Long connectionId, String searchText) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-options"),
            new OptionsRequest(
                componentName, componentVersion, triggerName, propertyName, inputParameters, connectionId,
                searchText),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public Property executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-output-schema"),
            new OutputSchemaRequest(componentName, componentVersion, triggerName, inputParameters, connectionId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public void executeDynamicWebhookDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId,
        @NonNull Map<String, ?> outputParameters,
        Long connectionId) {

        defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-dynamic-webhook-disable"),
            new DynamicWebhookDisableRequest(
                componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, outputParameters,
                connectionId));
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Long connectionId,
        @NonNull String webhookUrl) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-dynamic-webhook-enable"),
            new DynamicWebhookEnableRequest(
                componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, connectionId,
                webhookUrl),
            DynamicWebhookEnableOutput.class);
    }

    @Override
    public Object executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-sample-output"),
            new SampleOutputRequest(componentName, componentVersion, triggerName, inputParameters, connectionId),
            Object.class);
    }

    @Override
    public TriggerOutput executeTrigger(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Object triggerState, WebhookRequest webhookRequest,
        Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-trigger"),
            new TriggerRequest(
                componentName, componentVersion, triggerName, inputParameters, triggerState, webhookRequest,
                connectionId),
            TriggerOutput.class);
    }

    @Override
    public boolean executeWebhookValidate(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull WebhookRequest webhookRequest, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-webhook-validate"),
            new WebhookValidateRequest(
                componentName, componentVersion, triggerName, inputParameters, webhookRequest,
                connectionId),
            Boolean.class);
    }

    private record EditorDescriptionRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        Long connectionId) {
    }

    private record OptionsRequest(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, Long connectionId, String searchText) {
    }

    private record OutputSchemaRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        Long connectionId) {
    }

    private record PropertiesRequest(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, Long connectionId) {
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
        String componentName, int componentVersion, String triggerName, Map<String, ?> outputParameters) {
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
