
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

package com.bytechef.hermes.component.registry.remote.web.rest.service;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.component.registry.domain.ConnectionDefinition;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.service.RemoteConnectionDefinitionService;
import com.bytechef.hermes.connection.domain.Connection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/remote/connection-definition-service")
public class RemoteConnectionDefinitionServiceController {

    private final RemoteConnectionDefinitionService connectionDefinitionService;

    @SuppressFBWarnings("EI")
    public RemoteConnectionDefinitionServiceController(
        RemoteConnectionDefinitionService connectionDefinitionService) {

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
    public ResponseEntity<Authorization.ApplyResponse> executeAuthorizationApply(
        @RequestBody Connection connection) {

        return ResponseEntity.ok(
            connectionDefinitionService.executeAuthorizationApply(connection));
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

        ConnectionRequest connectionRequest = authorizationCallbackRequest.connection();

        return ResponseEntity.ok(
            connectionDefinitionService.executeAuthorizationCallback(
                connectionRequest.componentName, connectionRequest.connectionVersion, connectionRequest.parameters,
                connectionRequest.authorizationName, authorizationCallbackRequest.redirectUri()));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-base-uri")
    public ResponseEntity<String> executeBaseUri(@RequestBody Connection connection) {
        return connectionDefinitionService.executeBaseUri(connection)
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
            connectionDefinitionService.getAuthorizationType(componentName, connectionVersion, authorizationName));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-connection-definition/{componentName}/{componentVersion}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ConnectionDefinition> getConnectionDefinition(
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
    public ResponseEntity<List<ConnectionDefinition>> getConnectionDefinitions(
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
    public ResponseEntity<List<ConnectionDefinition>> getConnectionDefinitions() {
        return ResponseEntity.ok(connectionDefinitionService.getConnectionDefinitions());
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/get-oauth2-authorization-parameters",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<OAuth2AuthorizationParameters> getOAuth2AuthorizationParameters(
        @Valid @RequestBody RemoteConnectionDefinitionServiceController.ConnectionRequest connection) {

        return ResponseEntity.ok(
            connectionDefinitionService.getOAuth2AuthorizationParameters(
                connection.componentName, connection.connectionVersion, connection.parameters,
                connection.authorizationName));
    }

    @SuppressFBWarnings("EI")
    public record AuthorizationCallbackRequest(@NotNull ConnectionRequest connection, @NotNull String redirectUri) {
    }

    @SuppressFBWarnings("EI")
    public record ConnectionRequest(
        @NotNull String componentName, int connectionVersion, @NotNull Map<String, Object> parameters,
        @NotNull String authorizationName) {
    }
}
