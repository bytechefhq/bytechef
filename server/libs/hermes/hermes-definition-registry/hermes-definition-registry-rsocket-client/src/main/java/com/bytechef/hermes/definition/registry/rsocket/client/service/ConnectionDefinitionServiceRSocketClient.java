
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

import com.bytechef.commons.reactor.util.MonoUtils;
import com.bytechef.commons.discovery.util.DiscoveryUtils;
import com.bytechef.commons.rsocket.util.RSocketUtils;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationContext;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ConnectionDefinitionServiceRSocketClient implements ConnectionDefinitionService {

    private static final String WORKER_SERVICE_APP = "worker-service-app";

    private final DiscoveryClient discoveryClient;
    private final RSocketRequester.Builder rSocketRequesterBuilder;

    public ConnectionDefinitionServiceRSocketClient(
        DiscoveryClient discoveryClient, RSocketRequester.Builder rSocketRequesterBuilder) {

        this.discoveryClient = discoveryClient;
        this.rSocketRequesterBuilder = rSocketRequesterBuilder;
    }

    @Override
    public boolean connectionExists(String componentName, int connectionVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void executeAuthorizationApply(
        String componentName, int connectionVersion, Map<String, Object> connectionParameters, String authorizationName,
        AuthorizationContext authorizationContext) {

        Map<String, Map<String, List<String>>> authorizationContextMap = MonoUtils.get(
            getRSocketRequester(componentName)
                .route("ConnectionDefinitionService.executeAuthorizationApply")
                .data(new Connection(componentName, connectionVersion, connectionParameters, authorizationName))
                .retrieveMono(new ParameterizedTypeReference<>() {}));

        authorizationContext.setHeaders(authorizationContextMap.get("headers"));
        authorizationContext.setQueryParameters(authorizationContextMap.get("queryParameters"));
    }

    @Override
    public Authorization.AuthorizationCallbackResponse executeAuthorizationCallback(
        String componentName, int connectionVersion, Map<String, Object> connectionParameters, String authorizationName,
        String redirectUri) {

        return MonoUtils.get(
            getRSocketRequester(componentName)
                .route("ConnectionDefinitionService.executeAuthorizationCallback")
                .data(new AuthorizationCallbackRequest(
                    new Connection(componentName, connectionVersion, connectionParameters, authorizationName),
                    redirectUri))
                .retrieveMono(Authorization.AuthorizationCallbackResponse.class));
    }

    @Override
    public Optional<String> fetchBaseUri(
        String componentName, int connectionVersion, Map<String, Object> connectionParameters) {

        return Optional.ofNullable(
            MonoUtils.get(
                getRSocketRequester(componentName)
                    .route("ConnectionDefinitionService.fetchBaseUri")
                    .data(new Connection(componentName, connectionVersion, connectionParameters, null))
                    .retrieveMono(String.class)));
    }

    @Override
    public Authorization.AuthorizationType getAuthorizationType(
        String authorizationName, String componentName, int connectionVersion) {

        return MonoUtils.get(
            getRSocketRequester(componentName)
                .route("ConnectionDefinitionService.getAuthorizationType")
                .data(
                    Map.of(
                        "authorizationName", authorizationName,
                        "componentName", componentName,
                        "connectionVersion", connectionVersion))
                .retrieveMono(Authorization.AuthorizationType.class));
    }

    @Override
    public Mono<ConnectionDefinitionDTO> getConnectionDefinitionMono(
        String componentName, int componentVersion) {

        return getRSocketRequester(componentName)
            .route("ConnectionDefinitionService.getComponentConnectionDefinition")
            .data(Map.of("componentName", componentName, "componentVersion", componentVersion))
            .retrieveMono(ConnectionDefinitionDTO.class)
            .map(connectionDefinition -> connectionDefinition);
    }

    @Override
    public Mono<List<ConnectionDefinitionDTO>> getConnectionDefinitionsMono(
        String componentName, int componentVersion) {

        return Mono.zip(
            DiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP))
                .stream()
                .map(serviceInstance -> RSocketUtils.getRSocketRequester(serviceInstance, rSocketRequesterBuilder)
                    .route("ConnectionDefinitionService.getComponentConnectionDefinitions")
                    .data(Map.of("componentName", componentName, "componentVersion", componentVersion))
                    .retrieveMono(
                        new ParameterizedTypeReference<List<ConnectionDefinitionDTO>>() {}))
                .toList(),
            this::toConnectionDefinitions);
    }

    @Override
    public Mono<List<ConnectionDefinitionDTO>> getConnectionDefinitionsMono() {
        return Mono.zip(
            DiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP))
                .stream()
                .map(serviceInstance -> RSocketUtils.getRSocketRequester(serviceInstance, rSocketRequesterBuilder)
                    .route("ConnectionDefinitionService.getConnectionDefinitions")
                    .retrieveMono(
                        new ParameterizedTypeReference<List<ConnectionDefinitionDTO>>() {}))
                .toList(),
            this::toConnectionDefinitions);
    }

    @Override
    public OAuth2AuthorizationParametersDTO getOAuth2Parameters(
        String componentName, int connectionVersion, Map<String, Object> connectionParameters,
        String authorizationName) {

        return MonoUtils.get(
            getRSocketRequester(componentName)
                .route("ConnectionDefinitionService.getOAuth2Parameters")
                .data(new Connection(componentName, connectionVersion, connectionParameters, authorizationName))
                .retrieveMono(OAuth2AuthorizationParametersDTO.class));
    }

    private RSocketRequester getRSocketRequester(String componentName) {
        return RSocketUtils.getRSocketRequester(
            discoveryClient.getInstances(WORKER_SERVICE_APP), componentName, rSocketRequesterBuilder);
    }

    @SuppressWarnings("unchecked")
    private List<ConnectionDefinitionDTO> toConnectionDefinitions(Object[] objectArray) {
        return Arrays.stream(objectArray)
            .map(object -> (List<ConnectionDefinitionDTO>) object)
            .flatMap(Collection::stream)
            .toList();
    }

    private record AuthorizationCallbackRequest(Connection connection, String redirectUri) {
    }

    private record Connection(
        String componentName, int connectionVersion, Map<String, Object> parameters, String authorizationName) {
    }
}
