
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

package com.bytechef.hermes.definition.registry.service.web.rest.service;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class ConnectionDefinitionServiceController {

    private final ConnectionDefinitionService connectionDefinitionService;

    public ConnectionDefinitionServiceController(ConnectionDefinitionService connectionDefinitionService) {
        this.connectionDefinitionService = connectionDefinitionService;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/connection-definitions/authorization-apply",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<Map<String, Map<String, List<String>>>> executeAuthorizationApply(
        @Valid @RequestBody Connection connection) {
        Map<String, List<String>> headers = new HashMap<>();
        Map<String, List<String>> queryParameters = new HashMap<>();

        connectionDefinitionService.executeAuthorizationApply(
            connection.componentName, connection.connectionVersion, connection.parameters, connection.authorizationName,
            new AuthorizationContextImpl(headers, queryParameters, new HashMap<>()));

        return ResponseEntity.ok(Map.of("headers", headers, "queryParameters", queryParameters));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/connection-definitions/authorization-callback",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<AuthorizationCallbackResponse> executeAuthorizationCallback(
        @Valid @RequestBody AuthorizationCallbackRequest authorizationCallbackRequest) {

        Connection connection = authorizationCallbackRequest.connection();

        return ResponseEntity.ok(
            connectionDefinitionService.executeAuthorizationCallback(
                connection.componentName, connection.connectionVersion, connection.parameters,
                connection.authorizationName, authorizationCallbackRequest.redirectUri()));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/{componentName}/connection-definitions/{connectionVersion}/authorizations/{authorizationName}/authorization-type",
        produces = {
            "application/json"
        })
    public ResponseEntity<AuthorizationType> getAuthorizationType(
        @PathVariable("componentName") String componentName,
        @PathVariable("connectionVersion") Integer connectionVersion,
        @PathVariable("authorizationName") String authorizationName) {

        return ResponseEntity.ok(
            connectionDefinitionService.getAuthorizationType(authorizationName, componentName, connectionVersion));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/connection-definitions/base-uri")
    public ResponseEntity<String> getBaseUri(Connection connection) {
        return connectionDefinitionService.fetchBaseUri(
            connection.componentName, connection.connectionVersion, connection.parameters)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent()
                .build());
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/{componentName}/{componentVersion}/connection-definition",
        produces = {
            "application/json"
        })
    public ResponseEntity<ConnectionDefinitionDTO> getComponentConnectionDefinition(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion) {

        return ResponseEntity.ok(connectionDefinitionService.getConnectionDefinition(componentName, componentVersion));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/component-definitions/{componentName}/{componentVersion}/connection-definitions",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ConnectionDefinitionDTO>> getComponentConnectionDefinitions(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion) {

        return ResponseEntity.ok(connectionDefinitionService.getConnectionDefinitions(componentName, componentVersion));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/connection-definitions",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ConnectionDefinitionDTO>> getConnectionDefinitions() {
        return ResponseEntity.ok(connectionDefinitionService.getConnectionDefinitions());
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/connection-definitions/oauth2-parameters",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<OAuth2AuthorizationParametersDTO> getOAuth2Parameters(
        @Valid @RequestBody Connection connection) {

        return ResponseEntity.ok(connectionDefinitionService.getOAuth2Parameters(
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
        @NotNull String componentName, int connectionVersion, Map<String, Object> parameters,
        String authorizationName) {
    }
}
