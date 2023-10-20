
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

package com.bytechef.worker.config;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.ApplyResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.registry.domain.ConnectionDefinition;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.component.registry.service.RemoteConnectionDefinitionService;
import com.bytechef.hermes.component.registry.remote.client.service.RemoteConnectionDefinitionServiceClient;
import com.bytechef.hermes.connection.domain.Connection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
public class DefinitionRegistryConfiguration {

    @Bean("workerConnectionDefinitionService")
    @Primary
    RemoteConnectionDefinitionService connectionDefinitionService(
        @Qualifier("connectionDefinitionService") ConnectionDefinitionService connectionDefinitionService,
        RemoteConnectionDefinitionServiceClient connectionDefinitionServiceClient) {

        return new WorkerConnectionDefinitionService(connectionDefinitionService, connectionDefinitionServiceClient);
    }

    /**
     * Compound ConnectionDefinitionService impl that supports the use case where a component (for example, HttpClient
     * or Script) uses a compatible connection from a different component that can be in a different worker instance.
     */
    private static class WorkerConnectionDefinitionService implements RemoteConnectionDefinitionService {

        private final ConnectionDefinitionService connectionDefinitionService;
        private final RemoteConnectionDefinitionServiceClient connectionDefinitionServiceClient;

        public WorkerConnectionDefinitionService(
            ConnectionDefinitionService connectionDefinitionService,
            RemoteConnectionDefinitionServiceClient connectionDefinitionServiceClient) {

            this.connectionDefinitionService = connectionDefinitionService;
            this.connectionDefinitionServiceClient = connectionDefinitionServiceClient;
        }

        /**
         * Called from the Context.Connection instance.
         */
        @Override
        public ApplyResponse executeAuthorizationApply(@NonNull Connection connection) {
            if (connectionDefinitionService.connectionExists(
                connection.getComponentName(), connection.getConnectionVersion())) {

                return connectionDefinitionService.executeAuthorizationApply(connection);
            } else {
                return connectionDefinitionServiceClient.executeAuthorizationApply(connection);
            }
        }

        /**
         * Called from the ConnectionFacade instance.
         */
        @Override
        public AuthorizationCallbackResponse executeAuthorizationCallback(
            @NonNull String componentName, int connectionVersion, @NonNull Map<String, ?> connectionParameters,
            @NonNull String authorizationName, @NonNull String redirectUri) {

            return connectionDefinitionService.executeAuthorizationCallback(
                componentName, connectionVersion, connectionParameters, authorizationName, redirectUri);
        }

        /**
         * Called from the HttpClientExecutor instance.
         */
        @Override
        public Optional<String> executeBaseUri(@NonNull Connection connection) {
            if (connectionDefinitionService.connectionExists(
                connection.getComponentName(), connection.getConnectionVersion())) {

                return connectionDefinitionService.executeBaseUri(connection);
            } else {
                return connectionDefinitionServiceClient.executeBaseUri(connection);
            }
        }

        @Override
        public Authorization.AuthorizationType getAuthorizationType(
            @NonNull String componentName, int connectionVersion, @NonNull String authorizationName) {

            return connectionDefinitionService.getAuthorizationType(
                componentName, connectionVersion, authorizationName);
        }

        @Override
        public ConnectionDefinition getConnectionDefinition(@NonNull String componentName, int componentVersion) {
            return connectionDefinitionService.getConnectionDefinition(componentName, componentVersion);
        }

        @Override
        public List<ConnectionDefinition>
            getConnectionDefinitions(@NonNull String componentName, @NonNull Integer componentVersion) {
            return connectionDefinitionService.getConnectionDefinitions(componentName, componentVersion);
        }

        @Override
        public List<ConnectionDefinition> getConnectionDefinitions() {
            return connectionDefinitionService.getConnectionDefinitions();
        }

        @Override
        public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
            @NonNull String componentName, int connectionVersion, @NonNull Map<String, ?> connectionParameters,
            @NonNull String authorizationName) {

            return connectionDefinitionService.getOAuth2AuthorizationParameters(
                componentName, connectionVersion, connectionParameters, authorizationName);
        }
    }
}
