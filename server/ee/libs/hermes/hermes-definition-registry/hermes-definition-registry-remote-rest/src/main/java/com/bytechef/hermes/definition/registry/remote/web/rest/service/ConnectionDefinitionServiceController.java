
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

package com.bytechef.hermes.definition.registry.remote.web.rest.service;

import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.definition.Authorization.ApplyResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal/connection-definition-service")
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class ConnectionDefinitionServiceController {

    private final ConnectionDefinitionService connectionDefinitionService;

    public ConnectionDefinitionServiceController(ConnectionDefinitionService connectionDefinitionService) {
        this.connectionDefinitionService = connectionDefinitionService;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-authorization-apply",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<ApplyResponse> executeAuthorizationApply(
        @Valid @RequestBody Connection connection) {

        return ResponseEntity.ok(
            connectionDefinitionService.executeAuthorizationApply(
                connection.componentName, connection.connectionVersion, connection.parameters,
                connection.authorizationName));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-authorization-callback",
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
        method = RequestMethod.POST,
        value = "/execute-fetch-base-uri")
    public ResponseEntity<String> executeFetchBaseUri(Connection connection) {
        return connectionDefinitionService.executeFetchBaseUri(
            connection.componentName, connection.connectionVersion, connection.parameters)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent()
                .build());
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-authorization-type/{componentName}/{connectionVersion}/{authorizationName}",
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
        method = RequestMethod.GET,
        value = "/get-connection-definition/{componentName}/{componentVersion}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ConnectionDefinitionDTO> getConnectionDefinition(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion) {

        return ResponseEntity.ok(connectionDefinitionService.getConnectionDefinition(componentName, componentVersion));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-connection-definitions/{componentName}/{componentVersion}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ConnectionDefinitionDTO>> getConnectionDefinitions(
        @PathVariable("componentName") String componentName,
        @PathVariable("componentVersion") Integer componentVersion) {

        return ResponseEntity.ok(connectionDefinitionService.getConnectionDefinitions(componentName, componentVersion));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-connection-definitions",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ConnectionDefinitionDTO>> getConnectionDefinitions() {
        return ResponseEntity.ok(connectionDefinitionService.getConnectionDefinitions());
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/get-oauth2-parameters",
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

    @SuppressFBWarnings("EI")
    public record AuthorizationCallbackRequest(Connection connection, String redirectUri) {
    }

    @SuppressFBWarnings("EI")
    public record Connection(
        @NotNull String componentName, int connectionVersion, Map<String, Object> parameters,
        String authorizationName) {
    }
}
