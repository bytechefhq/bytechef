
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

package com.bytechef.hermes.definition.registry.service.web.rest.client.service;

import com.bytechef.hermes.definition.registry.dto.TaskDispatcherDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.TaskDispatcherDefinitionService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public class TaskDispatcherDefinitionServiceClient implements TaskDispatcherDefinitionService {

    private final WebClient.Builder coordinatorWebClientBuilder;

    public TaskDispatcherDefinitionServiceClient(WebClient.Builder coordinatorWebClientBuilder) {
        this.coordinatorWebClientBuilder = coordinatorWebClientBuilder;
    }

    @Override
    public TaskDispatcherDefinitionDTO getTaskDispatcherDefinition(String name, Integer version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<TaskDispatcherDefinitionDTO> getTaskDispatcherDefinitionMono(String name, Integer version) {
        return coordinatorWebClientBuilder
            .build()
            .get()
            .uri(uriBuilder -> uriBuilder
                .host("coordinator-service-app")
                .path("/api/internal/task-dispatcher-definitions/{name}/{version}")
                .build(name, version))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TaskDispatcherDefinitionDTO> getTaskDispatcherDefinitions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<List<TaskDispatcherDefinitionDTO>> getTaskDispatcherDefinitionsMono() {
        return coordinatorWebClientBuilder
            .build()
            .get()
            .uri(uriBuilder -> uriBuilder
                .host("coordinator-service-app")
                .path("/api/internal/task-dispatcher-definitions")
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TaskDispatcherDefinitionDTO> getTaskDispatcherDefinitionVersions(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<List<TaskDispatcherDefinitionDTO>> getTaskDispatcherDefinitionVersionsMono(String name) {
        return coordinatorWebClientBuilder
            .build()
            .get()
            .uri(uriBuilder -> uriBuilder
                .host("coordinator-service-app")
                .path("/api/internal/task-dispatcher-definitions/{name}/versions")
                .build(name))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<>() {});
    }
}
