/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.component.registry.remote.client.service;

import com.bytechef.commons.rest.client.DefaultRestClient;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.registry.domain.ActionDefinition;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.remote.client.AbstractWorkerClient;
import com.bytechef.platform.component.registry.service.ActionDefinitionService;
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
public class RemoteActionDefinitionServiceClient extends AbstractWorkerClient implements ActionDefinitionService {

    private static final String ACTION_DEFINITION_SERVICE = "/action-definition-service";

    public RemoteActionDefinitionServiceClient(
        DefaultRestClient defaultRestClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultRestClient, discoveryClient, objectMapper);
    }

    @Override
    public List<Property> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection connection, @NonNull ActionContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String executeNodeDescription(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters,
        @NonNull ActionContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, String searchText, ComponentConnection connection,
        @NonNull ActionContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Output executeOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection connection, @NonNull ActionContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, ?> executePerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters,
        ComponentConnection connection, @NonNull ActionContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ActionDefinition getActionDefinition(
        @NonNull String componentName, int componentVersion, @NonNull String actionName) {

        return defaultRestClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                ACTION_DEFINITION_SERVICE + "/get-action-definition/{componentName}/{componentVersion}/{actionName}",
                componentName, componentVersion, actionName),
            ActionDefinition.class);
    }

    @Override
    public List<ActionDefinition> getActionDefinitions(@NonNull String componentName, int componentVersion) {
        return defaultRestClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                ACTION_DEFINITION_SERVICE + "/get-action-definitions/{componentName}/{componentVersion}",
                componentName, componentVersion),
            new ParameterizedTypeReference<>() {});
    }
}
