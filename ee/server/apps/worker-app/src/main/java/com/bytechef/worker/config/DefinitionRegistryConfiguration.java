/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.worker.config;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.ConnectionDefinition;
import com.bytechef.platform.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.registry.remote.client.facade.RemoteConnectionDefinitionFacadeClient;
import com.bytechef.platform.component.registry.service.ConnectionDefinitionService;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class DefinitionRegistryConfiguration {

    @Bean("workerConnectionDefinitionService")
    @Primary
    ConnectionDefinitionService connectionDefinitionService(
        @Qualifier("connectionDefinitionService") ConnectionDefinitionService connectionDefinitionService,
        RemoteConnectionDefinitionFacadeClient remoteConnectionDefinitionFacadeClient) {

        return new WorkerConnectionDefinitionService(
            connectionDefinitionService, remoteConnectionDefinitionFacadeClient);
    }

    /**
     * Compound ConnectionDefinitionService impl that supports the use case where a component (for example, DataStream
     * or Script) uses a compatible connection from a different component that can be in a different worker instance
     * when .
     */
    private record WorkerConnectionDefinitionService(
        ConnectionDefinitionService connectionDefinitionService,
        RemoteConnectionDefinitionFacadeClient remoteConnectionDefinitionFacadeClient)
        implements ConnectionDefinitionService {

        /**
         * Called from the Context.Connection instance.
         */
        @Override
        public ApplyResponse executeAuthorizationApply(
            @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

            if (connectionDefinitionService.containsConnection(componentName)) {
                return connectionDefinitionService.executeAuthorizationApply(componentName, connection, context);
            } else {
                return remoteConnectionDefinitionFacadeClient.executeAuthorizationApply(componentName, connection);
            }
        }

        /**
         * Called from the ConnectionFacade instance.
         */
        @Override
        public AuthorizationCallbackResponse executeAuthorizationCallback(
            @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context,
            @NonNull String redirectUri) {

            return connectionDefinitionService.executeAuthorizationCallback(
                componentName, connection, context, redirectUri);
        }

        @Override
        public boolean containsConnection(String componentName) {
            return connectionDefinitionService.containsConnection(componentName);
        }

        /**
         * Called from the HttpClientExecutor instance.
         */
        @Override
        public Optional<String> executeBaseUri(
            @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

            if (connectionDefinitionService.containsConnection(componentName)) {
                return connectionDefinitionService.executeBaseUri(componentName, connection, context);
            } else {
                return remoteConnectionDefinitionFacadeClient.executeBaseUri(componentName, connection);
            }
        }

        @Override
        public Optional<ConnectionDefinition> fetchConnectionDefinition(
            @NonNull String componentName, int componentVersion) {

            return connectionDefinitionService.fetchConnectionDefinition(componentName, componentVersion);
        }

        @Override
        public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
            @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

            return connectionDefinitionService.getOAuth2AuthorizationParameters(componentName, connection, context);
        }

        @Override
        public AuthorizationType getAuthorizationType(
            @NonNull String componentName, int connectionVersion, @NonNull String authorizationName) {

            return connectionDefinitionService.getAuthorizationType(
                componentName, connectionVersion, authorizationName);
        }

        @Override
        public ConnectionDefinition getConnectionDefinition(@NonNull String componentName, int componentVersion) {
            return connectionDefinitionService.getConnectionDefinition(componentName, componentVersion);
        }

        @Override
        public List<ConnectionDefinition> getConnectionDefinitions() {
            return connectionDefinitionService.getConnectionDefinitions();
        }

        @Override
        public List<ConnectionDefinition> getConnectionDefinitions(
            @NonNull String componentName, @NonNull Integer componentVersion) {

            return connectionDefinitionService.getConnectionDefinitions(componentName, componentVersion);
        }
    }
}
