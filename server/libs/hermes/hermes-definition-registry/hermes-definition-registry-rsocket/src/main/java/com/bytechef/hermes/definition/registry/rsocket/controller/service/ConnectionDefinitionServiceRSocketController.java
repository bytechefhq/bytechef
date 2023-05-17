
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

package com.bytechef.hermes.definition.registry.rsocket.controller.service;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class ConnectionDefinitionServiceRSocketController {

    private final ConnectionDefinitionService connectionDefinitionService;

    public ConnectionDefinitionServiceRSocketController(ConnectionDefinitionService connectionDefinitionService) {
        this.connectionDefinitionService = connectionDefinitionService;
    }

    @MessageMapping("ConnectionDefinitionService.fetchBaseUri")
    public Mono<String> fetchBaseUri(Connection connection) {
        return connectionDefinitionService.fetchBaseUri(
            connection.componentName, connection.connectionVersion, connection.parameters)
            .map(Mono::just)
            .orElse(Mono.empty());
    }

    @MessageMapping("ConnectionDefinitionService.executeAuthorizationApply")
    public Mono<Map<String, Map<String, List<String>>>> executeAuthorizationApply(Connection connection) {
        Map<String, List<String>> headers = new HashMap<>();
        Map<String, List<String>> queryParameters = new HashMap<>();

        connectionDefinitionService.executeAuthorizationApply(
            connection.componentName, connection.connectionVersion, connection.parameters, connection.authorizationName,
            new AuthorizationContextImpl(headers, queryParameters, new HashMap<>()));

        return Mono.just(Map.of("headers", headers, "queryParameters", queryParameters));
    }

    @MessageMapping("ConnectionDefinitionService.executeAuthorizationCallback")
    public Mono<Authorization.AuthorizationCallbackResponse> executeAuthorizationCallback(
        AuthorizationCallbackRequest authorizationCallbackRequest) {

        Connection connection = authorizationCallbackRequest.connection();

        return Mono.just(
            connectionDefinitionService.executeAuthorizationCallback(
                connection.componentName, connection.connectionVersion, connection.parameters,
                connection.authorizationName, authorizationCallbackRequest.redirectUri()));
    }

    @MessageMapping("ConnectionDefinitionService.getAuthorizationType")
    public Mono<Authorization.AuthorizationType> getAuthorizationType(Map<String, Object> map) {
        return Mono.just(
            connectionDefinitionService.getAuthorizationType(
                (String) map.get("authorizationName"), (String) map.get("componentName"),
                (Integer) map.get("connectionVersion")));
    }

    @MessageMapping("ConnectionDefinitionService.getComponentConnectionDefinition")
    public Mono<ConnectionDefinitionDTO> getComponentConnectionDefinition(Map<String, Object> map) {
        return Mono.just(
            connectionDefinitionService.getConnectionDefinition(
                (String) map.get("componentName"), (Integer) map.get("componentVersion")));
    }

    @MessageMapping("ConnectionDefinitionService.getComponentConnectionDefinitions")
    public Mono<List<ConnectionDefinitionDTO>> getComponentConnectionDefinitions(Map<String, Object> map) {
        return Mono.just(
            connectionDefinitionService.getConnectionDefinitions(
                (String) map.get("componentName"), (Integer) map.get("componentVersion")));
    }

    @MessageMapping("ConnectionDefinitionService.getConnectionDefinitions")
    public Mono<List<ConnectionDefinitionDTO>> getConnectionDefinitions() {
        return Mono.just(connectionDefinitionService.getConnectionDefinitions());
    }

    @MessageMapping("ConnectionDefinitionService.getOAuth2Parameters")
    public Mono<OAuth2AuthorizationParametersDTO> getOAuth2Parameters(Connection connection) {
        return Mono.just(
            connectionDefinitionService.getOAuth2Parameters(
                connection.componentName, connection.connectionVersion, connection.parameters,
                connection.authorizationName));
    }

    record AuthorizationContextImpl(
        Map<String, List<String>> headers, Map<String, List<String>> queryParameters, Map<String, String> body)
        implements Authorization.AuthorizationContext {

        private static final Base64.Encoder ENCODER = Base64.getEncoder();

        @Override
        public void setHeaders(Map<String, List<String>> headers) {
            this.headers.putAll(headers);
        }

        @Override
        public void setQueryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters.putAll(queryParameters);
        }

        @Override
        public void setBody(Map<String, String> body) {
            this.body.putAll(body);
        }

        @Override
        public void setUsernamePassword(String username, String password) {
            String valueToEncode = username + ":" + password;

            headers.put(
                "Authorization",
                List.of("Basic " + ENCODER.encodeToString(valueToEncode.getBytes(StandardCharsets.UTF_8))));
        }
    }

    private record AuthorizationCallbackRequest(Connection connection, String redirectUri) {
    }

    private record Connection(
        String componentName, int connectionVersion, Map<String, Object> parameters, String authorizationName) {
    }
}
