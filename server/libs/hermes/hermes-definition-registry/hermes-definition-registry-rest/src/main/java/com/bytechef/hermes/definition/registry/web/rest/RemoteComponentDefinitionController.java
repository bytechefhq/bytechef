
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
import com.bytechef.hermes.definition.registry.web.rest.model.ActionDefinitionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentDefinitionWithBasicActionsModel;
import com.bytechef.hermes.definition.registry.web.rest.utils.ServiceInstanceUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnApi
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "platform-service-app")
@RequestMapping("${openapi.openAPIDefinition.base-path:}")
public class RemoteComponentDefinitionController implements ComponentDefinitionsApi {

    public static final WebClient WEB_CLIENT = WebClient.builder()
        .build();

    private final DiscoveryClient discoveryClient;

    public RemoteComponentDefinitionController(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public Mono<ResponseEntity<ActionDefinitionModel>> getActionDefinition(
        String componentName, Integer componentVersion, String actionName, ServerWebExchange exchange) {

        return WEB_CLIENT.get()
            .uri(
                ServiceInstanceUtils.toComponentDefinitionActionUri(
                    ServiceInstanceUtils.filterServiceInstance(
                        discoveryClient.getInstances("worker-service-app"), componentName)),
                Map.of("componentName", componentName, "componentVersion", componentVersion, "actionName", actionName))
            .retrieve()
            .bodyToMono(ActionDefinitionModel.class)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<ComponentDefinitionWithBasicActionsModel>> getComponentDefinition(
        String name, Integer version, ServerWebExchange exchange) {

        return WEB_CLIENT.get()
            .uri(
                ServiceInstanceUtils.toComponentDefinitionUri(
                    ServiceInstanceUtils.filterServiceInstance(
                        discoveryClient.getInstances("worker-service-app"), name)),
                Map.of("name", name, "version", version))
            .retrieve()
            .bodyToMono(ComponentDefinitionWithBasicActionsModel.class)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<ComponentDefinitionBasicModel>>> getComponentDefinitions(
        Boolean connectionDefinitions, Boolean connectionInstances, ServerWebExchange exchange) {

        return Mono.just(
            ResponseEntity.ok(
                Flux.fromIterable(
                    ServiceInstanceUtils.filterServiceInstances(discoveryClient.getInstances("worker-service-app")))
                    .flatMap(serviceInstance -> WEB_CLIENT.get()
                        .uri(
                            ServiceInstanceUtils.toComponentDefinitionsUri(
                                serviceInstance, connectionDefinitions, connectionInstances))
                        .retrieve()
                        .bodyToFlux(ComponentDefinitionBasicModel.class))

            ));
    }

    @Override
    public Mono<ResponseEntity<Flux<ComponentDefinitionBasicModel>>> getComponentDefinitionVersions(
        String name, ServerWebExchange exchange) {

        return Mono.just(
            ResponseEntity.ok(
                Flux.fromIterable(
                    ServiceInstanceUtils.filterServiceInstances(discoveryClient.getInstances("worker-service-app")))
                    .flatMap(serviceInstance -> WEB_CLIENT.get()
                        .uri(
                            ServiceInstanceUtils.toComponentDefinitionVersionsUri(serviceInstance),
                            Map.of("name", name))
                        .retrieve()
                        .bodyToFlux(ComponentDefinitionBasicModel.class))

            ));
    }
}
