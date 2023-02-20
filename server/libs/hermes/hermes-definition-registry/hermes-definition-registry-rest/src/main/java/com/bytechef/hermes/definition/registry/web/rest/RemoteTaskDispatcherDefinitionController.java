
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

package com.bytechef.hermes.definition.registry.web.rest;

import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.hermes.definition.registry.web.rest.model.TaskDispatcherDefinitionModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnApi
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "platform-service-app")
@RequestMapping("${openapi.openAPIDefinition.base-path:}")
public class RemoteTaskDispatcherDefinitionController implements TaskDispatcherDefinitionsApi {

    private final WebClient.Builder builder;

    public RemoteTaskDispatcherDefinitionController(WebClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public Mono<ResponseEntity<Flux<TaskDispatcherDefinitionModel>>> getTaskDispatcherDefinitions(
        ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(
                builder.build()
                    .get()
                    .uri("http://coordinator-service-app/api/task-dispatcher-definitions")
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .bodyToFlux(TaskDispatcherDefinitionModel.class)));
    }
}
