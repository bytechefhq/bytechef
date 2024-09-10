/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.registry.remote.client.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Authorization.RefreshTokenResponse;
import com.bytechef.component.definition.Context;
import com.bytechef.ee.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.ee.platform.component.registry.remote.client.AbstractWorkerClient;
import com.bytechef.ee.remote.client.DefaultRestClient;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.ConnectionDefinition;
import com.bytechef.platform.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.registry.service.ConnectionDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

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
        DefaultRestClient defaultRestClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultRestClient, discoveryClient, objectMapper);
    }

    @Override
    public Map<String, ?> executeAcquire(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName,
        @NonNull Map<String, ?> connectionParameters, @NonNull Context context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ApplyResponse executeAuthorizationApply(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName,
        @NonNull Map<String, ?> connectionParameters, @NonNull Context context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AuthorizationCallbackResponse executeAuthorizationCallback(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName,
        @NonNull Map<String, ?> connectionParameters, @NonNull Context context, @NonNull String redirectUri) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> executeBaseUri(
        @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public RefreshTokenResponse executeRefresh(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName,
        @NonNull Map<String, ?> connectionParameters, @NonNull Context context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName,
        @NonNull Map<String, ?> connectionParameters, @NonNull Context context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getAuthorizationDetectOn(
        @NonNull String componentName, int componentVersion, @NonNull String authorizationName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Object> getAuthorizationRefreshOn(
        @NonNull String componentName, int componentVersion, @NonNull String authorizationName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AuthorizationType getAuthorizationType(
        @NonNull String componentName, int connectionVersion, @NonNull String authorizationName) {

        return defaultRestClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                CONNECTION_DEFINITION_SERVICE + "/get-authorization-type/{componentName}/{connectionVersion}" +
                    "/{authorizationName}",
                componentName, connectionVersion, authorizationName),
            AuthorizationType.class);
    }

    @Override
    public ConnectionDefinition
        getConnectionConnectionDefinition(@NonNull String componentName, int connectionVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionDefinition getConnectionDefinition(@NonNull String componentName, int componentVersion) {
        return defaultRestClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                CONNECTION_DEFINITION_SERVICE + "/get-connection-definition/{componentName}/{componentVersion}",
                componentName, componentVersion),
            ConnectionDefinition.class);
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions(
        @NonNull String componentName, @NonNull Integer componentVersion) {

        List<CompletableFuture<List<ConnectionDefinition>>> completableFutures = CollectionUtils.map(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_APP)),
            serviceInstance -> CompletableFuture.supplyAsync(() -> defaultRestClient.get(
                uriBuilder -> toUri(
                    uriBuilder, serviceInstance,
                    CONNECTION_DEFINITION_SERVICE + "/get-connection-definitions/{componentName}/{componentVersion}",
                    componentName, componentVersion),
                new ParameterizedTypeReference<>() {})));

        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]))
            .join();

        return completableFutures.stream()
            .map(CompletableFuture::join)
            .flatMap(CollectionUtils::stream)
            .collect(Collectors.toList());
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions() {
        List<CompletableFuture<ConnectionDefinition>> completableFutures = CollectionUtils.map(
            WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_APP)),
            serviceInstance -> CompletableFuture.supplyAsync(() -> defaultRestClient.get(
                uriBuilder -> toUri(
                    uriBuilder, serviceInstance, CONNECTION_DEFINITION_SERVICE + "/get-connection-definitions"),
                ConnectionDefinition.class)));

        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]))
            .join();

        return CollectionUtils.map(completableFutures, CompletableFuture::join);
    }
}
