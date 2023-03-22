
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

package com.bytechef.hermes.definition.registry.rsocket.service;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

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
        return connectionDefinitionService.fetchBaseUri(connection)
            .map(Mono::just)
            .orElse(Mono.empty());
    }

    @MessageMapping("ConnectionDefinitionService.executeAuthorizationApply")
    public Mono<Map<String, Map<String, List<String>>>> executeAuthorizationApply(Connection connection) {
        Map<String, List<String>> returnHeaders = new HashMap<>();
        Map<String, List<String>> returnQueryParameters = new HashMap<>();

        connectionDefinitionService.executeAuthorizationApply(connection, new Authorization.AuthorizationContext() {

            @Override
            public void setHeaders(Map<String, List<String>> headers) {
                returnHeaders.putAll(headers);
            }

            @Override
            public void setQueryParameters(Map<String, List<String>> queryParameters) {
                returnQueryParameters.putAll(queryParameters);
            }
        });

        return Mono.just(Map.of("headers", returnHeaders, "queryParameters", returnQueryParameters));
    }

    @MessageMapping("ConnectionDefinitionService.executeAuthorizationCallback")
    public Mono<Authorization.AuthorizationCallbackResponse> executeAuthorizationCallback(Map<String, Object> map) {
        return Mono.just(
            connectionDefinitionService.executeAuthorizationCallback(
                (Connection) map.get("connection"), (String) map.get("redirectUri")));
    }

    @MessageMapping("ConnectionDefinitionService.getAuthorization")
    public Mono<Authorization> getAuthorization(Map<String, Object> map) {
        return Mono.just(
            connectionDefinitionService.getAuthorization(
                (String) map.get("authorizationName"), (String) map.get("componentName"),
                (Integer) map.get("connectionVersion")));
    }

    @MessageMapping("ConnectionDefinitionService.getComponentConnectionDefinition")
    public Mono<ConnectionDefinition> getComponentConnectionDefinition(Map<String, Object> map) {
        return connectionDefinitionService.getComponentConnectionDefinitionMono((String) map.get("componentName"),
            (Integer) map.get("componentVersion"));
    }

    @MessageMapping("ConnectionDefinitionService.getComponentConnectionDefinitions")
    public Mono<List<ConnectionDefinition>> getComponentConnectionDefinitions(Map<String, Object> map) {
        return connectionDefinitionService.getComponentConnectionDefinitionsMono(
            (String) map.get("componentName"), (Integer) map.get("componentVersion"));
    }

    @MessageMapping("ConnectionDefinitionService.getConnectionDefinitions")
    public Mono<List<ConnectionDefinition>> getConnectionDefinitions() {
        return connectionDefinitionService.getConnectionDefinitionsMono();
    }

    @MessageMapping("ConnectionDefinitionService.getOAuth2Parameters")
    public Mono<ConnectionDefinitionService.OAuth2AuthorizationParameters> getOAuth2Parameters(Connection connection) {
        return Mono.just(connectionDefinitionService.getOAuth2Parameters(connection));
    }
}
