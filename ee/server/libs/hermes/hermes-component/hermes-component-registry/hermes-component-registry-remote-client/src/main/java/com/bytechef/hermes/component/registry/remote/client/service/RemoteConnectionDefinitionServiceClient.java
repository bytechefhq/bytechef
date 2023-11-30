/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.component.registry.remote.client.service;

import com.bytechef.commons.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.component.definition.Authorization.ApplyResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.registry.domain.ConnectionDefinition;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.dto.ComponentConnection;
import com.bytechef.hermes.component.registry.remote.client.AbstractWorkerClient;
import com.bytechef.hermes.component.registry.service.ConnectionDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectionDefinitionServiceClient extends AbstractWorkerClient
    implements ConnectionDefinitionService {

    private static final String CONNECTION_DEFINITION_SERVICE = "/connection-definition-service";

    public RemoteConnectionDefinitionServiceClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultWebClient, discoveryClient, objectMapper);
    }

    @Override
    public ApplyResponse executeAuthorizationApply(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AuthorizationCallbackResponse executeAuthorizationCallback(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context,
        @NonNull String redirectUri) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean connectionExists(String componentName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> executeBaseUri(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AuthorizationType getAuthorizationType(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName) {

        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                CONNECTION_DEFINITION_SERVICE + "/get-authorization-type/{componentName}/{connectionVersion}" +
                    "/{authorizationName}",
                componentName, connectionVersion, authorizationName),
            AuthorizationType.class);
    }

    @Override
    public ConnectionDefinition getConnectionDefinition(@NonNull String componentName, int componentVersion) {
        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                CONNECTION_DEFINITION_SERVICE + "/get-connection-definition/{componentName}/{componentVersion}",
                componentName, componentVersion),
            ConnectionDefinition.class);
    }

    @Override
    public List<ConnectionDefinition>
        getConnectionDefinitions(@NonNull String componentName, @NonNull Integer componentVersion) {
        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_APP), objectMapper)
                .stream()
                .map(serviceInstance -> defaultWebClient.getMono(
                    uriBuilder -> toUri(
                        uriBuilder, serviceInstance,
                        CONNECTION_DEFINITION_SERVICE
                            + "/get-connection-definitions/{componentName}/{componentVersion}",
                        componentName, componentVersion),
                    new ParameterizedTypeReference<List<ConnectionDefinition>>() {}))
                .toList(),
            this::toConnectionDefinitions)
            .block();
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions() {
        return Mono.zip(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_APP), objectMapper)
                .stream()
                .map(serviceInstance -> defaultWebClient.getMono(
                    uriBuilder -> toUri(
                        uriBuilder, serviceInstance, CONNECTION_DEFINITION_SERVICE + "/get-connection-definitions"),
                    ConnectionDefinition.class))
                .toList(),
            this::toConnectionDefinitions)
            .block();
    }

    @SuppressWarnings("unchecked")
    private List<ConnectionDefinition> toConnectionDefinitions(Object[] objectArray) {
        return Arrays.stream(objectArray)
            .map(object -> (List<ConnectionDefinition>) object)
            .flatMap(Collection::stream)
            .toList();
    }
}
