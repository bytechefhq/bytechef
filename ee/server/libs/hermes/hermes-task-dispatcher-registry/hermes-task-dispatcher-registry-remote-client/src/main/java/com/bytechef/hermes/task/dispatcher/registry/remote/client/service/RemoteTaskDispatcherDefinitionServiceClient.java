/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.task.dispatcher.registry.remote.client.service;

import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.hermes.task.dispatcher.registry.domain.TaskDispatcherDefinition;
import com.bytechef.hermes.task.dispatcher.registry.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public RemoteTaskDispatcherDefinitionServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public TaskDispatcherDefinition getTaskDispatcherDefinition(String name, Integer version) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(COORDINATOR_APP)
                .path(
                    TASK_DISPATCHER_DEFINITION_SERVICE + "/get-task-dispatcher-definition/{name}/{version}")
                .build(name, version),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TaskDispatcherDefinition> getTaskDispatcherDefinitions() {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(COORDINATOR_APP)
                .path(TASK_DISPATCHER_DEFINITION_SERVICE + "/get-task-dispatcher-definitions")
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TaskDispatcherDefinition> getTaskDispatcherDefinitionVersions(String name) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(COORDINATOR_APP)
                .path(TASK_DISPATCHER_DEFINITION_SERVICE + "/get-task-dispatcher-definition-versions/{name}")
                .build(name),
            new ParameterizedTypeReference<>() {});
    }
}
