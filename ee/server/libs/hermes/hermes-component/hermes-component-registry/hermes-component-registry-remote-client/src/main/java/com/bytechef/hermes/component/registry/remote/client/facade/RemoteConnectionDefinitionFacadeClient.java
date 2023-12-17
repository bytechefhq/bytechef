/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.component.registry.remote.client.facade;

import com.bytechef.commons.restclient.DefaultRestClient;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.domain.ComponentConnection;
import com.bytechef.hermes.component.registry.facade.ConnectionDefinitionFacade;
import com.bytechef.hermes.component.registry.remote.client.AbstractWorkerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
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
    public Authorization.ApplyResponse executeAuthorizationApply(
        @NonNull String componentName, @NonNull ComponentConnection connection) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/execute-authorization-apply"),
            new ConnectionRequest(componentName, connection), Authorization.ApplyResponse.class);
    }

    @Override
    public Authorization.AuthorizationCallbackResponse executeAuthorizationCallback(
        @NonNull String componentName,
        @NonNull ComponentConnection connection, @NonNull String redirectUri) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/execute-authorization-callback"),
            new AuthorizationCallbackRequest(componentName, connection, redirectUri),
            Authorization.AuthorizationCallbackResponse.class);
    }

    @Override
    public Optional<String> executeBaseUri(
        @NonNull String componentName, ComponentConnection connection) {
        return Optional.ofNullable(
            defaultRestClient.post(
                uriBuilder -> toUri(uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/execute-base-uri"),
                new ConnectionRequest(componentName, connection), String.class));
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        @NonNull String componentName, ComponentConnection connection) {

        return defaultRestClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/get-oauth2-authorization-parameters"),
            new ConnectionRequest(componentName, connection),
            OAuth2AuthorizationParameters.class);
    }

    private record AuthorizationCallbackRequest(
        String componentName, ComponentConnection connection, String redirectUri) {
    }

    private record ConnectionRequest(String componentName, ComponentConnection connection) {
    }
}
