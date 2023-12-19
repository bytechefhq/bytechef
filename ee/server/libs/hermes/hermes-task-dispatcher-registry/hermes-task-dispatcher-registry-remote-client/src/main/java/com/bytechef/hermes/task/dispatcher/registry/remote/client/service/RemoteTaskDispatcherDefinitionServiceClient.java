/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.task.dispatcher.registry.remote.client.service;

import com.bytechef.commons.rest.client.LoadBalancedRestClient;
import com.bytechef.hermes.task.dispatcher.registry.domain.TaskDispatcherDefinition;
import com.bytechef.hermes.task.dispatcher.registry.domain.ValueProperty;
import com.bytechef.hermes.task.dispatcher.registry.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTaskDispatcherDefinitionServiceClient implements TaskDispatcherDefinitionService {

    private static final String COORDINATOR_APP = "coordinator-app";
    private static final String TASK_DISPATCHER_DEFINITION_SERVICE = "/remote/task-dispatcher-definition-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteTaskDispatcherDefinitionServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        String name, int version, Map<String, Object> inputParameters) {

        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(COORDINATOR_APP)
                .path(
                    TASK_DISPATCHER_DEFINITION_SERVICE + "/execute-output-schema")
                .build(),
            new OutputSchemaRequest(name, version, inputParameters),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public TaskDispatcherDefinition getTaskDispatcherDefinition(String name, Integer version) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(COORDINATOR_APP)
                .path(
                    TASK_DISPATCHER_DEFINITION_SERVICE + "/get-task-dispatcher-definition/{name}/{version}")
                .build(name, version),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TaskDispatcherDefinition> getTaskDispatcherDefinitions() {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(COORDINATOR_APP)
                .path(TASK_DISPATCHER_DEFINITION_SERVICE + "/get-task-dispatcher-definitions")
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TaskDispatcherDefinition> getTaskDispatcherDefinitionVersions(String name) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(COORDINATOR_APP)
                .path(TASK_DISPATCHER_DEFINITION_SERVICE + "/get-task-dispatcher-definition-versions/{name}")
                .build(name),
            new ParameterizedTypeReference<>() {});
    }

    private record OutputSchemaRequest(String name, int version, Map<String, ?> taskDispatcherParameters) {
    }
}
