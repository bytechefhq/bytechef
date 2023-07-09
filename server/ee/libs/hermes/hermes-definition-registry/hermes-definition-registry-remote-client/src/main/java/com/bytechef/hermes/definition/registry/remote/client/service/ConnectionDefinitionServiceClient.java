
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

import com.bytechef.commons.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.definition.Authorization.ApplyResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import com.bytechef.hermes.definition.registry.remote.client.AbstractWorkerClient;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ConnectionDefinitionServiceClient extends AbstractWorkerClient
    implements ConnectionDefinitionService {

    public ConnectionDefinitionServiceClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultWebClient, discoveryClient, objectMapper);
    }

    @Override
    public boolean connectionExists(String componentName, int connectionVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressFBWarnings("NP")
    public ApplyResponse executeAuthorizationApply(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, "/connection-definition-service/execute-authorization-apply"),
            new Connection(componentName, connectionVersion, connectionParameters, authorizationName),
            ApplyResponse.class);
    }

    @Override
    public AuthorizationCallbackResponse executeAuthorizationCallback(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName,
        String redirectUri) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, "/connection-definition-service/execute-authorization-callback"),
            new AuthorizationCallbackRequest(
                new Connection(componentName, connectionVersion, connectionParameters, authorizationName),
                redirectUri),
            AuthorizationCallbackResponse.class);
    }

    @Override
    public Optional<String> fetchBaseUri(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters) {

        return Optional.ofNullable(
            defaultWebClient.post(
                uriBuilder -> toUri(uriBuilder, componentName, "/connection-definition-service/fetch-base-uri"),
                new Connection(componentName, connectionVersion, connectionParameters, null),
                String.class));
    }

    @Override
    public AuthorizationType getAuthorizationType(
        String authorizationName, String componentName, int connectionVersion) {

        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                "/connection-definition-service/get-authorization-type/{componentName}/{connectionVersion}" +
                    "/{authorizationName}",
                componentName, connectionVersion, authorizationName),
            AuthorizationType.class);
    }

    @Override
    public ConnectionDefinitionDTO getConnectionDefinition(String componentName, int componentVersion) {
        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                "/connection-definition-service/get-connection-definition/{componentName}/{componentVersion}",
                componentName, componentVersion),
            ConnectionDefinitionDTO.class);
    }

    @Override
    public List<ConnectionDefinitionDTO> getConnectionDefinitions(String componentName, Integer componentVersion) {
        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP), objectMapper)
                .stream()
                .map(serviceInstance -> defaultWebClient.getMono(
                    uriBuilder -> toUri(
                        uriBuilder, serviceInstance,
                        "/connection-definition-service/get-connection-definitions/{componentName}/{componentVersion}",
                        componentName, componentVersion),
                    new ParameterizedTypeReference<List<ConnectionDefinitionDTO>>() {}))
                .toList(),
            this::toConnectionDefinitions)
            .block();
    }

    @Override
    public List<ConnectionDefinitionDTO> getConnectionDefinitions() {
        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP), objectMapper)
                .stream()
                .map(serviceInstance -> defaultWebClient.getMono(
                    uriBuilder -> toUri(
                        uriBuilder, serviceInstance, "/connection-definition-service/get-connection-definitions"),
                    ConnectionDefinitionDTO.class))
                .toList(),
            this::toConnectionDefinitions)
            .block();
    }

    @Override
    public OAuth2AuthorizationParametersDTO getOAuth2Parameters(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName) {

        return defaultWebClient.post(
            uriBuilder -> toUri(uriBuilder, componentName, "/connection-definition-service/get-oauth2-parameters"),
            new Connection(componentName, connectionVersion, connectionParameters, authorizationName),
            OAuth2AuthorizationParametersDTO.class);
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
        String componentName, int connectionVersion, Map<String, ?> parameters, String authorizationName) {
    }
}
