
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

package com.bytechef.hermes.definition.registry.remote.web.rest.client.service;

import com.bytechef.commons.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationContext;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.definition.registry.remote.web.rest.client.AbstractWorkerClient;
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

    public ConnectionDefinitionServiceClient(DiscoveryClient discoveryClient) {
        super(discoveryClient);
    }

    @Override
    public boolean connectionExists(String componentName, int connectionVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressFBWarnings("NP")
    public void executeAuthorizationApply(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName,
        AuthorizationContext authorizationContext) {

        Map<String, Map<String, List<String>>> authorizationContextMap = WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/connection-definitions/authorization-apply"))
            .bodyValue(new Connection(componentName, connectionVersion, connectionParameters, authorizationName))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Map<String, List<String>>>>() {})
            .block();

        authorizationContext.setHeaders(authorizationContextMap.get("headers"));
        authorizationContext.setQueryParameters(authorizationContextMap.get("queryParameters"));
    }

    @Override
    public AuthorizationCallbackResponse executeAuthorizationCallback(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName,
        String redirectUri) {

        return WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/connection-definitions/authorization-callback"))
            .bodyValue(
                new AuthorizationCallbackRequest(
                    new Connection(componentName, connectionVersion, connectionParameters, authorizationName),
                    redirectUri))
            .retrieve()
            .bodyToMono(AuthorizationCallbackResponse.class)
            .block();
    }

    @Override
    public Optional<String> fetchBaseUri(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters) {

        return Optional.ofNullable(
            WORKER_WEB_CLIENT
                .post()
                .uri(uriBuilder -> toUri(uriBuilder, componentName, "/connection-definitions/base-uri"))
                .bodyValue(new Connection(componentName, connectionVersion, connectionParameters, null))
                .retrieve()
                .bodyToMono(String.class)
                .block());
    }

    @Override
    public AuthorizationType getAuthorizationType(
        String authorizationName, String componentName, int connectionVersion) {

        return WORKER_WEB_CLIENT
            .get()
            .uri(uriBuilder -> toUri(
                uriBuilder, componentName,
                "/component-definitions/{componentName}/connection-definitions/{connectionVersion}/" +
                    "authorizations/{authorizationName}/authorization-type",
                componentName, connectionVersion,
                authorizationName))
            .retrieve()
            .bodyToMono(AuthorizationType.class)
            .block();
    }

    @Override
    public ConnectionDefinitionDTO getConnectionDefinition(String componentName, int componentVersion) {
        return WORKER_WEB_CLIENT
            .get()
            .uri(uriBuilder -> toUri(
                uriBuilder, componentName,
                "/component-definitions/{componentName}/{componentVersion}/connection-definition", componentName,
                componentVersion))
            .retrieve()
            .bodyToMono(ConnectionDefinitionDTO.class)
            .block();
    }

    @Override
    public List<ConnectionDefinitionDTO> getConnectionDefinitions(String componentName, int componentVersion) {
        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP))
                .stream()
                .map(serviceInstance -> WORKER_WEB_CLIENT
                    .get()
                    .uri(uriBuilder -> toUri(
                        uriBuilder, serviceInstance,
                        "/component-definitions/{componentName}/{componentVersion}/connection-definitions",
                        componentName, componentVersion))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ConnectionDefinitionDTO>>() {}))
                .toList(),
            this::toConnectionDefinitions)
            .block();
    }

    @Override
    public List<ConnectionDefinitionDTO> getConnectionDefinitions() {
        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_SERVICE_APP))
                .stream()
                .map(serviceInstance -> WORKER_WEB_CLIENT
                    .get()
                    .uri(uriBuilder -> toUri(uriBuilder, serviceInstance, "/component-definitions"))
                    .retrieve()
                    .bodyToMono(ConnectionDefinitionDTO.class))
                .toList(),
            this::toConnectionDefinitions)
            .block();
    }

    @Override
    public OAuth2AuthorizationParametersDTO getOAuth2Parameters(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName) {

        return WORKER_WEB_CLIENT
            .post()
            .uri(uriBuilder -> toUri(uriBuilder, componentName, "/connection-definitions/oauth2-parameters"))
            .bodyValue(new Connection(componentName, connectionVersion, connectionParameters, authorizationName))
            .retrieve()
            .bodyToMono(OAuth2AuthorizationParametersDTO.class)
            .block();
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
