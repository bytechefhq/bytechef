/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.task.dispatcher.registry.remote.client.service;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
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
    public OutputResponse executeOutput(
        String name, int version, Map<String, ?> inputParameters) {

        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(COORDINATOR_APP)
                .path(
                    TASK_DISPATCHER_DEFINITION_SERVICE + "/execute-output")
                .build(),
            new OutputRequest(name, version, inputParameters),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public OutputResponse executeVariableProperties(String name, int version, Map<String, ?> inputParameters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String executeWorkflowNodeDescription(String name, int version, Map<String, ?> inputParameters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TaskDispatcherDefinition> fetchTaskDispatcherDefinition(String name, @Nullable Integer version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskDispatcherDefinition getTaskDispatcherDefinition(String name, @Nullable Integer version) {
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

    @Override
    public boolean isDynamicOutputDefined(String componentName, int componentVersion) {
        throw new UnsupportedOperationException();
    }

    private record OutputRequest(String name, int version, Map<String, ?> taskDispatcherParameters) {
    }
}
