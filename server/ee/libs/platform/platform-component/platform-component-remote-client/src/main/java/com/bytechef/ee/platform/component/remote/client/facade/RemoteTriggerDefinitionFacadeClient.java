/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.ee.platform.component.remote.client.AbstractWorkerClient;
import com.bytechef.ee.remote.client.DefaultRestClient;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.domain.OutputResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
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
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-properties"),
            new PropertiesRequest(
                componentName, componentVersion, triggerName, propertyName, inputParameters, connectionId,
                lookupDependsOnPaths),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public WebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> outputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-dynamic-webhook-refresh"),
            new DynamicWebhookRefresh(componentName, componentVersion, triggerName, outputParameters, connectionId),
            WebhookEnableOutput.class);
    }

    @Override
    public void executeListenerDisable(
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, String workflowExecutionId, Long connectionId) {

        defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-listener-disable"),
            new ListenerDisableRequest(
                componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, connectionId));
    }

    @Override
    public void executeListenerEnable(
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, String workflowExecutionId, Long connectionId) {

        defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-listener-enable"),
            new ListenerEnableRequest(
                componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, connectionId));
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
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
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-output"),
            new OutputRequest(componentName, componentVersion, triggerName, inputParameters, connectionId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public TriggerOutput executeTrigger(
        String componentName, int componentVersion, String triggerName, Long jobPrincipalId, String workflowUuid,
        Map<String, ?> inputParameters, Object triggerState, WebhookRequest webhookRequest, Long connectionId,
        Long environmentId, ModeType type, boolean editorEnvironment) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, TRIGGER_DEFINITION_FACADE + "/execute-trigger"),
            new TriggerRequest(
                componentName, componentVersion, triggerName, inputParameters, triggerState, webhookRequest,
                connectionId, jobPrincipalId, workflowUuid, editorEnvironment, type, environmentId),
            TriggerOutput.class);
    }

    @Override
    public void executeWebhookDisable(
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, String workflowExecutionId,
        Map<String, ?> outputParameters,
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
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, String workflowExecutionId, Long connectionId,
        String webhookUrl) {

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
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, WebhookRequest webhookRequest, Long connectionId) {

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
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, WebhookRequest webhookRequest, Long connectionId) {

        throw new UnsupportedOperationException();
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
        WebhookRequest webhookRequest, Long connectionId, Long jobPrincipalId, String workflowUuid,
        boolean editorEnvironment, ModeType type, Long environmentId) {
    }

    private record WebhookValidateRequest(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, Long connectionId) {
    }
}
