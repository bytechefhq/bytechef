
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
import com.bytechef.commons.util.DiscoveryUtils;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    public void executeAuthorizationApply(
        Connection connection, Authorization.AuthorizationContext authorizationContext) {

        Map<String, Map<String, List<String>>> authorizationContextMap = MonoUtils.get(
            rSocketRequesterBuilder
                .websocket(getWebsocketUri(connection))
                .route("ConnectionDefinitionService.executeAuthorizationApply")
                .data(connection)
                .retrieveMono(new ParameterizedTypeReference<>() {}));

        authorizationContext.setHeaders(authorizationContextMap.get("header"));
        authorizationContext.setQueryParameters(authorizationContextMap.get("queryParameters"));
    }

    @Override
    public Authorization.AuthorizationCallbackResponse executeAuthorizationCallback(
        Connection connection, String redirectUri) {

        return MonoUtils.get(
            rSocketRequesterBuilder
                .websocket(getWebsocketUri(connection))
                .route("ConnectionDefinitionService.executeAuthorizationCallback")
                .data(Map.of("connection", connection, "redirectUri", redirectUri))
                .retrieveMono(Authorization.AuthorizationCallbackResponse.class));
    }

    @Override
    public Optional<String> fetchBaseUri(Connection connection) {
        return Optional.ofNullable(
            MonoUtils.get(
                rSocketRequesterBuilder
                    .websocket(getWebsocketUri(connection))
                    .route("ConnectionDefinitionService.fetchBaseUri")
                    .data(connection)
                    .retrieveMono(String.class)));
    }

    @Override
    public Authorization.AuthorizationType getAuthorizationType(
        String authorizationName, String componentName, int connectionVersion) {
        return MonoUtils.get(
            rSocketRequesterBuilder
                .websocket(getWebsocketUri(componentName))
                .route("ConnectionDefinitionService.getAuthorizationType")
                .data(
                    Map.of(
                        "authorizationName", authorizationName,
                        "componentName", componentName,
                        "connectionVersion", connectionVersion))
    @Override
    public ConnectionDefinition getComponentConnectionDefinition(String componentName, int componentVersion) {
        return MonoUtils.get(getComponentConnectionDefinitionMono(componentName, componentVersion));
    }

    @Override
    public Mono<ConnectionDefinition> getComponentConnectionDefinitionMono(String componentName, int componentVersion) {
        return rSocketRequesterBuilder
            .websocket(DiscoveryUtils.toWebSocketUri(
                DiscoveryUtils.filterServiceInstance(
                    discoveryClient.getInstances(WORKER_SERVICE_APP), componentName)))
            .route("ConnectionDefinitionService.getComponentConnectionDefinition")
            .data(Map.of("componentName", componentName, "componentVersion", componentVersion))
            .retrieveMono(ConnectionDefinition.class)
            .map(connectionDefinition -> connectionDefinition);
    }

    @Override
    public Mono<List<ConnectionDefinition>> getComponentConnectionDefinitionsMono(
        String componentName, int componentVersion) {
        return Mono.zip(
            DiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP))
                .stream()
                .map(serviceInstance -> rSocketRequesterBuilder
                    .websocket(DiscoveryUtils.toWebSocketUri(serviceInstance))
                    .route("ConnectionDefinitionService.getComponentConnectionDefinitions")
                    .data(Map.of("componentName", componentName, "componentVersion", componentVersion))
                    .retrieveMono(new ParameterizedTypeReference<List<ConnectionDefinition>>() {}))
                .toList(),
            this::toConnectionDefinitions);
    }

    @Override
    public Mono<List<ConnectionDefinition>> getConnectionDefinitionsMono() {
        return Mono.zip(
            DiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP))
                .stream()
                .map(serviceInstance -> rSocketRequesterBuilder
                    .websocket(DiscoveryUtils.toWebSocketUri(serviceInstance))
                    .route("ConnectionDefinitionService.getConnectionDefinitions")
                    .retrieveMono(new ParameterizedTypeReference<List<ConnectionDefinition>>() {}))
                .toList(),
            this::toConnectionDefinitions);
    }

    @Override
    public OAuth2AuthorizationParametersDTO getOAuth2Parameters(Connection connection) {
        return MonoUtils.get(
            rSocketRequesterBuilder
                .websocket(getWebsocketUri(connection))
                .route("ConnectionDefinitionService.getOAuth2Parameters")
                .data(connection)
                .retrieveMono(OAuth2AuthorizationParametersDTO.class));
    }

    private URI getWebsocketUri(Connection connection) {
        return getWebsocketUri(connection.getComponentName());
    }

    private URI getWebsocketUri(String componentName) {
        return DiscoveryUtils.toWebSocketUri(
            DiscoveryUtils.filterServiceInstance(
                discoveryClient.getInstances("worker-service-app"), componentName));
    }

    @SuppressWarnings("unchecked")
    private List<ConnectionDefinition> toConnectionDefinitions(Object[] objectArray) {
        return Arrays.stream(objectArray)
            .map(object -> (List<ConnectionDefinition>) object)
            .flatMap(Collection::stream)
            .toList();
    }
}
