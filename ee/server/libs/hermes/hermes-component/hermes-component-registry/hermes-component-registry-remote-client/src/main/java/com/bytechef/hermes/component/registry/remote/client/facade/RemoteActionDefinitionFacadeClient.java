/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.component.registry.remote.client.facade;

import com.bytechef.commons.restclient.DefaultRestClient;
import com.bytechef.hermes.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.hermes.component.registry.remote.client.AbstractWorkerClient;
import com.bytechef.hermes.registry.domain.OptionsResponse;
import com.bytechef.hermes.registry.domain.ValueProperty;
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
public class RemoteActionDefinitionFacadeClient extends AbstractWorkerClient
    implements ActionDefinitionFacade {

    private static final String ACTION_DEFINITION_FACADE = "/action-definition-facade";

    public RemoteActionDefinitionFacadeClient(
        DefaultRestClient defaultRestClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultRestClient, discoveryClient, objectMapper);
    }

    @Override
    public String executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, Object> inputParameters,
        Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-editor-description"),
            new EditorDescriptionRequest(
                actionName, inputParameters, componentName, componentVersion, connectionId),
            String.class);
    }

    @Override
    public OptionsResponse executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, Object> inputParameters, Long connectionId, String searchText) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-options"),
            new OptionsRequest(
                actionName, propertyName, inputParameters, componentName, componentVersion, connectionId,
                searchText),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, Object> inputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-output-schema"),
            new OutputSchemaRequest(
                actionName, inputParameters, componentName, componentVersion, connectionId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<? extends ValueProperty<?>> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        Map<String, Object> inputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-dynamic-properties"),
            new PropertiesRequest(
                actionName, inputParameters, componentName, componentVersion, connectionId, propertyName),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public Object executePerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, int type, Long instanceId,
        @NonNull String workflowId, long taskExecutionId, @NonNull Map<String, ?> inputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-perform"),
            new PerformRequest(
                componentName, componentVersion, actionName, type, instanceId, workflowId, taskExecutionId,
                inputParameters, connectionId),
            Object.class);
    }

    @Override
    public Object executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, Object> inputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-sample-output"),
            new SampleOutputRequest(actionName, inputParameters, componentName, componentVersion, connectionId),
            Object.class);
    }

    private record EditorDescriptionRequest(
        String actionName, Map<String, Object> inputParameters, String componentName, int componentVersion,
        Long connectionId) {
    }

    private record OptionsRequest(
        String actionName, String propertyName, Map<String, Object> inputParameters, String componentName,
        int componentVersion, Long connectionId, String searchText) {
    }

    private record OutputSchemaRequest(
        String actionName, Map<String, Object> inputParameters, String componentName, int componentVersion,
        Long connectionId) {
    }

    private record PerformRequest(
        String componentName, int componentVersion, String actionName, int type, Long instanceId, String workflowId,
        long taskExecutionId, Map<String, ?> inputParameters, Long connectionId) {
    }

    private record PropertiesRequest(
        String actionName, Map<String, Object> inputParameters, String componentName, int componentVersion,
        Long connectionId, String propertyName) {
    }

    private record SampleOutputRequest(
        String actionName, Map<String, Object> inputParameters, String componentName, int componentVersion,
        Long connectionId) {
    }
}
