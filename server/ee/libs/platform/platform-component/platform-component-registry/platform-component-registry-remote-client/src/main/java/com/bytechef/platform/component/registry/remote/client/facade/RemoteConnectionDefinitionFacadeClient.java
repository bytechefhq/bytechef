/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.component.registry.remote.client.facade;

import com.bytechef.component.definition.Authorization;
import com.bytechef.platform.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.registry.facade.ConnectionDefinitionFacade;
import com.bytechef.platform.component.registry.remote.client.AbstractWorkerClient;
import com.bytechef.remote.client.DefaultRestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectionDefinitionFacadeClient extends AbstractWorkerClient
    implements ConnectionDefinitionFacade {

    private static final String CONNECTION_DEFINITION_FACADE = "/connection-definition-facade";

    public RemoteConnectionDefinitionFacadeClient(
        DefaultRestClient defaultRestClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultRestClient, discoveryClient, objectMapper);
    }

    @Override
    public Authorization.AuthorizationCallbackResponse executeAuthorizationCallback(
        @NonNull String componentName, @NonNull String authorizationName, @NonNull Map<String, ?> authorizationParams,
        @NonNull String redirectUri) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/execute-authorization-callback"),
            new AuthorizationCallbackRequest(componentName, authorizationName, authorizationParams, redirectUri),
            Authorization.AuthorizationCallbackResponse.class);
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        @NonNull String componentName, @NonNull String authorizationName, @NonNull Map<String, ?> authorizationParams) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/get-oauth2-authorization-parameters"),
            new ConnectionRequest(componentName, authorizationName, authorizationParams),
            OAuth2AuthorizationParameters.class);
    }

    private record AuthorizationCallbackRequest(
        String componentName, String authorizationName, Map<String, ?> authorizationParams, String redirectUri) {
    }

    private record ConnectionRequest(String componentName, String authorizationName,
        Map<String, ?> authorizationParams) {
    }
}
