
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

package com.bytechef.hermes.definition.registry.rsocket.client.service;

import com.bytechef.commons.util.MonoUtils;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.definition.registry.rsocket.client.util.ServiceInstanceUtils;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component
public class ConnectionDefinitionServiceRSocketClient implements ConnectionDefinitionService {

    private static final String WORKER_SERVICE_APP = "worker-service-app";

    private final DiscoveryClient discoveryClient;
    private final RSocketRequester.Builder rSocketRequesterBuilder;

    public ConnectionDefinitionServiceRSocketClient(
        DiscoveryClient discoveryClient,
        @Qualifier("workerRSocketRequesterBuilder") RSocketRequester.Builder rSocketRequesterBuilder) {

        this.discoveryClient = discoveryClient;
        this.rSocketRequesterBuilder = rSocketRequesterBuilder;
    }

    @Override
    public void applyAuthorization(Connection connection, Authorization.AuthorizationContext authorizationContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> fetchBaseUri(Connection connection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<ConnectionDefinition> getConnectionDefinitionMono(String componentName) {
        return rSocketRequesterBuilder
            .websocket(ServiceInstanceUtils.toWebSocketUri(
                ServiceInstanceUtils.filterServiceInstance(
                    discoveryClient.getInstances(WORKER_SERVICE_APP), componentName)))
            .route("ConnectionDefinitionService.getConnectionDefinition")
            .data(componentName)
            .retrieveMono(ConnectionDefinition.class)
            .map(connectionDefinition -> connectionDefinition);
    }

    @Override
    public Mono<List<ConnectionDefinition>> getConnectionDefinitionsMono() {
        return Mono.zip(
            ServiceInstanceUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP))
                .stream()
                .map(serviceInstance -> rSocketRequesterBuilder
                    .websocket(ServiceInstanceUtils.toWebSocketUri(serviceInstance))
                    .route("ConnectionDefinitionService.getConnectionDefinitions")
                    .retrieveMono(new ParameterizedTypeReference<List<ConnectionDefinition>>() {}))
                .toList(),
            ServiceInstanceUtils::toConnectionDefinitions);
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions(String componentName) {
        return MonoUtils.get(getConnectionDefinitionsMono(componentName));
    }

    @Override
    public Mono<List<ConnectionDefinition>> getConnectionDefinitionsMono(String componentName) {
        return Mono.zip(
            ServiceInstanceUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP))
                .stream()
                .map(serviceInstance -> rSocketRequesterBuilder
                    .websocket(ServiceInstanceUtils.toWebSocketUri(serviceInstance))
                    .route("ConnectionDefinitionService.getComponentConnectionDefinitions")
                    .data(componentName)
                    .retrieveMono(new ParameterizedTypeReference<List<ConnectionDefinition>>() {}))
                .toList(),
            ServiceInstanceUtils::toConnectionDefinitions);
    }
}
