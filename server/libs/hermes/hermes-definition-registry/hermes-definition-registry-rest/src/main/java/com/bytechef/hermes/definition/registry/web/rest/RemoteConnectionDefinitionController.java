
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
import com.bytechef.hermes.definition.registry.web.rest.model.ConnectionDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ConnectionDefinitionModel;
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
public class RemoteConnectionDefinitionController implements ConnectionDefinitionsApi {

    public static final WebClient WEB_CLIENT = WebClient.builder()
        .build();

    private final DiscoveryClient discoveryClient;

    public RemoteConnectionDefinitionController(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public Mono<ResponseEntity<Flux<ConnectionDefinitionModel>>> getConnectionDefinition(
        String componentName, Integer componentVersion, ServerWebExchange exchange) {

        return Mono.just(
            ResponseEntity.ok(
                WEB_CLIENT.get()
                    .uri(
                        ServiceInstanceUtils.toConnectionDefinitionUri(
                            ServiceInstanceUtils.filterServiceInstance(
                                discoveryClient.getInstances("worker-service-app"), componentName)),
                        Map.of("componentName", componentName, "componentVersion", componentVersion))
                    .retrieve()
                    .bodyToFlux(ConnectionDefinitionModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Flux<ConnectionDefinitionBasicModel>>> getConnectionDefinitions(
        ServerWebExchange exchange) {

        return Mono.just(
            ResponseEntity.ok(
                Flux.fromIterable(
                    ServiceInstanceUtils.filterServiceInstances(discoveryClient.getInstances("worker-service-app")))
                    .flatMap(serviceInstance -> WEB_CLIENT.get()
                        .uri(ServiceInstanceUtils.toConnectionDefinitionsUri(serviceInstance))
                        .retrieve()
                        .bodyToFlux(ConnectionDefinitionBasicModel.class))

            ));
    }
}
