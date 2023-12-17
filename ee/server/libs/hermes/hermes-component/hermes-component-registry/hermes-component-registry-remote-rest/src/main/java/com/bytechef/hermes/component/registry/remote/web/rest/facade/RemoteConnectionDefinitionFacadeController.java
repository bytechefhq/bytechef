/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.component.registry.remote.web.rest.facade;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.domain.ComponentConnection;
import com.bytechef.hermes.component.registry.facade.ConnectionDefinitionFacade;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/remote/connection-definition-facade")
public class RemoteConnectionDefinitionFacadeController {

    private final ConnectionDefinitionFacade connectionDefinitionFacade;

    public RemoteConnectionDefinitionFacadeController(
        @Qualifier("connectionDefinitionFacade") ConnectionDefinitionFacade connectionDefinitionFacade) {

        this.connectionDefinitionFacade = connectionDefinitionFacade;
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
            connectionDefinitionFacade.executeAuthorizationApply(
                connectionRequest.componentName, Validate.notNull(connectionRequest.connection, "connection")));
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
            connectionDefinitionFacade.executeAuthorizationCallback(
                authorizationCallbackRequest.componentName, authorizationCallbackRequest.connection,
                authorizationCallbackRequest.redirectUri()));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-base-uri")
    public ResponseEntity<String> executeBaseUri(@RequestBody ConnectionRequest connectionRequest) {
        return connectionDefinitionFacade.executeBaseUri(
            connectionRequest.componentName, connectionRequest.connection)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity
                .noContent()
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
            connectionDefinitionFacade.getOAuth2AuthorizationParameters(
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
