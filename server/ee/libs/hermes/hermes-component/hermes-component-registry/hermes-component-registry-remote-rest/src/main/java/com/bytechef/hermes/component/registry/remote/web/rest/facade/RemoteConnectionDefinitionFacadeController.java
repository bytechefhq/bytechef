
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

package com.bytechef.hermes.component.registry.remote.web.rest.facade;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.dto.ComponentConnection;
import com.bytechef.hermes.component.registry.facade.RemoteConnectionDefinitionFacade;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/remote/connection-definition-facade")
public class RemoteConnectionDefinitionFacadeController {

    private final RemoteConnectionDefinitionFacade remoteConnectionDefinitionFacade;

    public RemoteConnectionDefinitionFacadeController(
        @Qualifier("connectionDefinitionFacade") RemoteConnectionDefinitionFacade remoteConnectionDefinitionFacade) {

        this.remoteConnectionDefinitionFacade = remoteConnectionDefinitionFacade;
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
        @RequestBody ConnectionRequest connectionRequest) {

        return ResponseEntity.ok(
            remoteConnectionDefinitionFacade.executeAuthorizationApply(
                connectionRequest.componentName, Objects.requireNonNull(connectionRequest.connection)));
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
    public ResponseEntity<Authorization.AuthorizationCallbackResponse> executeAuthorizationCallback(
        @Valid @RequestBody AuthorizationCallbackRequest authorizationCallbackRequest) {

        return ResponseEntity.ok(
            remoteConnectionDefinitionFacade.executeAuthorizationCallback(
                authorizationCallbackRequest.componentName, authorizationCallbackRequest.connection,
                authorizationCallbackRequest.redirectUri()));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-base-uri")
    public ResponseEntity<String> executeBaseUri(@RequestBody ConnectionRequest connectionRequest) {
        return remoteConnectionDefinitionFacade.executeBaseUri(
            connectionRequest.componentName, connectionRequest.connection)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent()
                .build());
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
        @Valid @RequestBody ConnectionRequest connectionRequest) {

        return ResponseEntity.ok(
            remoteConnectionDefinitionFacade.getOAuth2AuthorizationParameters(
                connectionRequest.componentName, connectionRequest.connection));
    }

    @SuppressFBWarnings("EI")
    public record AuthorizationCallbackRequest(
        @NotNull String componentName, ComponentConnection connection, @NotNull String redirectUri) {
    }

    @SuppressFBWarnings("EI")
    public record ConnectionRequest(@NotNull String componentName, @Nullable ComponentConnection connection) {
    }
}
