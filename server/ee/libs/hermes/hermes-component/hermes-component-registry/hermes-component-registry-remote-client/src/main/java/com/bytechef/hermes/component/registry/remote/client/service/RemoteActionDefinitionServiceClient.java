
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.component.registry.remote.client.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.registry.domain.ActionDefinition;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.component.registry.dto.ComponentConnection;
import com.bytechef.hermes.component.registry.remote.client.AbstractWorkerClient;
import com.bytechef.hermes.component.registry.service.ActionDefinitionService;
import com.bytechef.hermes.registry.domain.Option;
import com.bytechef.hermes.registry.domain.ValueProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnMissingClass(value = "com.bytechef.worker.WorkerApplication")
public class RemoteActionDefinitionServiceClient extends AbstractWorkerClient implements ActionDefinitionService {

    private static final String ACTION_DEFINITION_SERVICE = "/action-definition-service";

    public RemoteActionDefinitionServiceClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultWebClient, discoveryClient, objectMapper);
    }

    @Override
    public List<? extends ValueProperty<?>> executeDynamicProperties(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, ComponentConnection connection, ActionContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String executeEditorDescription(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        ComponentConnection connection, ActionContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, String searchText, ComponentConnection connection, ActionContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        ComponentConnection connection, ActionContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executePerform(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        ComponentConnection connection, ActionContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeSampleOutput(
        String componentName, int componentVersion, String actionName, Map<String, ?> actionParameters,
        ComponentConnection connection, ActionContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ActionDefinition getActionDefinition(
        @NonNull String componentName, int componentVersion, @NonNull String actionName) {

        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                ACTION_DEFINITION_SERVICE + "/get-action-definition/{componentName}/{componentVersion}/{actionName}",
                componentName, componentVersion, actionName),
            ActionDefinition.class);
    }

    @Override
    public List<ActionDefinition> getActionDefinitions(@NonNull String componentName, int componentVersion) {
        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                ACTION_DEFINITION_SERVICE + "/get-action-definitions/{componentName}/{componentVersion}",
                componentName, componentVersion),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<ActionDefinition> getActionDefinitions(@NonNull List<ComponentOperation> componentOperations) {
        return CollectionUtils.map(
            componentOperations,
            componentOperation -> getActionDefinition(
                componentOperation.componentName(), componentOperation.componentVersion(),
                componentOperation.operationName()));
    }
}
