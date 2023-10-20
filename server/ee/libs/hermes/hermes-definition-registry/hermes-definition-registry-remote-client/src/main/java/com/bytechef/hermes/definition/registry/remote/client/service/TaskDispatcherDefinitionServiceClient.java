
/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.hermes.definition.registry.remote.client.service;

import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.hermes.definition.registry.domain.TaskDispatcherDefinition;
import com.bytechef.hermes.definition.registry.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public class TaskDispatcherDefinitionServiceClient implements TaskDispatcherDefinitionService {

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public TaskDispatcherDefinitionServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public TaskDispatcherDefinition getTaskDispatcherDefinition(String name, Integer version) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("coordinator-service-app")
                .path(
                    "/api/internal/task-dispatcher-definition-service/get-task-dispatcher-definition/{name}/{version}")
                .build(name, version),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TaskDispatcherDefinition> getTaskDispatcherDefinitions() {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("coordinator-service-app")
                .path("/api/internal/task-dispatcher-definition-service/get-task-dispatcher-definitions")
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TaskDispatcherDefinition> getTaskDispatcherDefinitionVersions(String name) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("coordinator-service-app")
                .path("/api/internal/task-dispatcher-definition-service/get-task-dispatcher-definition-versions/{name}")
                .build(name),
            new ParameterizedTypeReference<>() {});
    }
}
