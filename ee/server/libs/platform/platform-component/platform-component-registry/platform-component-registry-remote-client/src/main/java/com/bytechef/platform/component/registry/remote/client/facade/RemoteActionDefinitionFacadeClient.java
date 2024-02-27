/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.component.registry.remote.client.facade;

import com.bytechef.commons.rest.client.DefaultRestClient;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.registry.remote.client.AbstractWorkerClient;
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
    public List<Property> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        Map<String, ?> inputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-dynamic-properties"),
            new PropertiesRequest(
                componentName, componentVersion, actionName, inputParameters, connectionId, propertyName),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, Long connectionId, String searchText) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-options"),
            new OptionsRequest(
                componentName, componentVersion, actionName, propertyName, inputParameters, connectionId,
                searchText),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public Output executeOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-output-schema"),
            new OutputRequest(
                componentName, componentVersion, actionName, inputParameters, connectionId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public Map<String, ?> executePerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, int type, Long instanceId,
        @NonNull String workflowId, Long jobId, @NonNull Map<String, ?> inputParameters, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-perform"),
            new PerformRequest(
                componentName, componentVersion, actionName, type, instanceId, workflowId, jobId,
                inputParameters, connectionId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public String executeWorkflowNodeDescription(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-workflow-node-description"),
            new NodeDescriptionRequest(
                componentVersion, componentName, actionName, inputParameters),
            String.class);
    }

    private record NodeDescriptionRequest(
        int componentVersion, String componentName, String actionName, Map<String, ?> inputParameters) {
    }

    private record OptionsRequest(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, Long connectionId, String searchText) {
    }

    private record OutputRequest(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Long connectionId) {
    }

    private record PerformRequest(
        String componentName, int componentVersion, String actionName, int type, Long instanceId, String workflowId,
        long jobId, Map<String, ?> inputParameters, Long connectionId) {
    }

    private record PropertiesRequest(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Long connectionId, String propertyName) {
    }
}
