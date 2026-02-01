/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.ee.platform.component.remote.client.AbstractWorkerClient;
import com.bytechef.ee.remote.client.DefaultRestClient;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteActionDefinitionServiceClient extends AbstractWorkerClient implements ActionDefinitionService {

    private static final String ACTION_DEFINITION_SERVICE = "/action-definition-service";

    public RemoteActionDefinitionServiceClient(
        DefaultRestClient defaultRestClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultRestClient, discoveryClient, objectMapper);
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String workflowId,
        @Nullable ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public OutputResponse executeOutput(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Map<String, ComponentConnection> componentConnections) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executePerform(
        String componentName, int componentVersion, String actionName, Long jobPrincipalId, Long jobPrincipalWorkflowId,
        Long jobId, String workflowId, Map<String, ?> inputParameters,
        Map<String, ComponentConnection> componentConnections, Map<String, ?> extensions, Long environmentId,
        boolean editorEnvironment, PlatformType type) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executePerformForPolyglot(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        ComponentConnection componentConnection, Long environmentId, ActionContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, int connectionVersion, String componentOperationName,
        int statusCode, Object body) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ActionDefinition getActionDefinition(
        String componentName, int componentVersion, String actionName) {

        return defaultRestClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                ACTION_DEFINITION_SERVICE + "/get-action-definition/{componentName}/{componentVersion}/{actionName}",
                componentName, componentVersion, actionName),
            ActionDefinition.class);
    }

    @Override
    public List<ActionDefinition> getActionDefinitions(String componentName, int componentVersion) {
        return defaultRestClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                ACTION_DEFINITION_SERVICE + "/get-action-definitions/{componentName}/{componentVersion}",
                componentName, componentVersion),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public boolean isDynamicOutputDefined(String componentName, int componentVersion, String actionName) {
        throw new UnsupportedOperationException();
    }
}
