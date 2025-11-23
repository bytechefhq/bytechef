/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.ee.platform.component.remote.client.AbstractWorkerClient;
import com.bytechef.ee.remote.client.DefaultRestClient;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
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
public class RemoteActionDefinitionFacadeClient extends AbstractWorkerClient
    implements ActionDefinitionFacade {

    private static final String ACTION_DEFINITION_FACADE = "/action-definition-facade";

    public RemoteActionDefinitionFacadeClient(
        DefaultRestClient defaultRestClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultRestClient, discoveryClient, objectMapper);
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String workflowId, Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-dynamic-properties"),
            new PropertiesRequest(
                componentName, componentVersion, actionName, inputParameters, connectionId, propertyName),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        Long connectionId) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-options"),
            new OptionsRequest(
                componentName, componentVersion, actionName, propertyName, inputParameters, connectionId,
                lookupDependsOnPaths, searchText),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public OutputResponse executeOutput(
        String componentName, int componentVersion, String actionName,
        Map<String, ?> inputParameters, Map<String, Long> connectionIds) {

        return defaultRestClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-output-schema"),
            new OutputRequest(componentName, componentVersion, actionName, inputParameters, connectionIds),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public Map<String, ?> executePerform(
        String componentName, int componentVersion, String actionName, ModeType type,
        Long jobPrincipalId, Long jobPrincipalWorkflowId, Long jobId, String workflowId, Map<String, ?> inputParameters,
        Map<String, Long> connectionIds, Map<String, ?> extensions, boolean editorEnvironment) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, ACTION_DEFINITION_FACADE + "/execute-perform"),
            new PerformRequest(
                componentName, componentVersion, actionName, type, jobPrincipalId, jobPrincipalWorkflowId, jobId,
                inputParameters, connectionIds),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public Object executePerformForPolyglot(
        String componentName, int componentVersion, String actionName,
        Map<String, ?> inputParameters,
        ComponentConnection componentConnection, ActionContext actionContext) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String actionName, int statusCode,
        Object body) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String actionName,
        Map<String, ?> inputParameters) {

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
        Map<String, ?> inputParameters, Long connectionId, List<String> lookupDependsOnPaths, String searchText) {
    }

    private record OutputRequest(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Map<String, Long> connectionIds) {
    }

    private record PerformRequest(
        String componentName, int componentVersion, String actionName, ModeType type, Long jobPrincipalId,
        Long jobPrincipalWorkflowId, long jobId, Map<String, ?> inputParameters, Map<String, Long> connectionIds) {
    }

    private record PropertiesRequest(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Long connectionId, String propertyName) {
    }
}
