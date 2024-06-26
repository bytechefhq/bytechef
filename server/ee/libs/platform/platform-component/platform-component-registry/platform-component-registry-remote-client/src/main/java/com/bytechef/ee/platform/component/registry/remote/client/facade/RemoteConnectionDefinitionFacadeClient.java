/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.registry.remote.client.facade;

import com.bytechef.component.definition.Authorization;
import com.bytechef.ee.platform.component.registry.remote.client.AbstractWorkerClient;
import com.bytechef.ee.remote.client.DefaultRestClient;
import com.bytechef.platform.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.registry.facade.ConnectionDefinitionFacade;
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
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName,
        @NonNull Map<String, ?> authorizationParams, @NonNull String redirectUri) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/execute-authorization-callback"),
            new AuthorizationCallbackRequest(
                componentName, connectionVersion, authorizationName, authorizationParams, redirectUri),
            Authorization.AuthorizationCallbackResponse.class);
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName,
        @NonNull Map<String, ?> authorizationParams) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/get-oauth2-authorization-parameters"),
            new ConnectionRequest(componentName, connectionVersion, authorizationName, authorizationParams),
            OAuth2AuthorizationParameters.class);
    }

    private record AuthorizationCallbackRequest(
        String componentName, int connectionVersion, String authorizationName, Map<String, ?> authorizationParams,
        String redirectUri) {
    }

    private record ConnectionRequest(
        String componentName, int connectionVersion, String authorizationName, Map<String, ?> authorizationParams) {
    }
}
