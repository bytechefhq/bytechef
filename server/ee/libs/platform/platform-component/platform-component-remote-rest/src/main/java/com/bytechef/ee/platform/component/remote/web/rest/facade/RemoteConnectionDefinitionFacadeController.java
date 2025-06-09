/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.web.rest.facade;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.facade.ConnectionDefinitionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import java.util.Map;
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
                authorizationCallbackRequest.componentName, authorizationCallbackRequest.connectionVersion,
                authorizationCallbackRequest.authorizationType(), authorizationCallbackRequest.connectionParameters(),
                authorizationCallbackRequest.redirectUri()));
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

        if (connectionRequest == null) {
            return ResponseEntity.badRequest()
                .build();
        }

        return ResponseEntity.ok(
            connectionDefinitionFacade.getOAuth2AuthorizationParameters(
                connectionRequest.componentName, connectionRequest.connectionVersion,
                connectionRequest.authorizationType(), connectionRequest.connectionParameters()));
    }

    @SuppressFBWarnings("EI")
    public record AuthorizationCallbackRequest(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters, String redirectUri) {
    }

    @SuppressFBWarnings("EI")
    public record ConnectionRequest(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters) {
    }
}
