
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
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.registry.domain.ConnectionDefinition;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.dto.ComponentConnection;
import com.bytechef.hermes.component.registry.remote.client.facade.RemoteConnectionDefinitionFacadeClient;
import com.bytechef.hermes.component.registry.service.ConnectionDefinitionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

@Configuration
public class DefinitionRegistryConfiguration {

    @Bean("workerConnectionDefinitionService")
    @Primary
    ConnectionDefinitionService connectionDefinitionService(
        @Qualifier("connectionDefinitionService") ConnectionDefinitionService connectionDefinitionService,
        RemoteConnectionDefinitionFacadeClient connectionDefinitionFacadeClient) {

        return new WorkerConnectionDefinitionService(connectionDefinitionService, connectionDefinitionFacadeClient);
    }

    /**
     * Compound ConnectionDefinitionService impl that supports the use case where a component (for example, HttpClient
     * or Script) uses a compatible connection from a different component that can be in a different worker instance.
     */
    private record WorkerConnectionDefinitionService(
        ConnectionDefinitionService connectionDefinitionService,
        RemoteConnectionDefinitionFacadeClient connectionDefinitionFacadeClient)
        implements ConnectionDefinitionService {

        /**
         * Called from the Context.Connection instance.
         */
        @Override
        public ApplyResponse executeAuthorizationApply(
            @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

            if (connectionDefinitionService.connectionExists(componentName, connection.version())) {
                return connectionDefinitionService.executeAuthorizationApply(componentName, connection, context);
            } else {
                return connectionDefinitionFacadeClient.executeAuthorizationApply(componentName, connection);
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
        public boolean connectionExists(String componentName, int connectionVersion) {
            return connectionDefinitionService.connectionExists(componentName, connectionVersion);
        }

        /**
         * Called from the HttpClientExecutor instance.
         */
        @Override
        public Optional<String> executeBaseUri(
            @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

            if (connectionDefinitionService.connectionExists(componentName, connection.version())) {
                return connectionDefinitionService.executeBaseUri(componentName, connection, context);
            } else {
                return connectionDefinitionFacadeClient.executeBaseUri(componentName, connection);
            }
        }

        @Override
        public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
            @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

            return connectionDefinitionService.getOAuth2AuthorizationParameters(componentName, connection, context);
        }

        @Override
        public Authorization.AuthorizationType getAuthorizationType(
            String componentName, int connectionVersion, String authorizationName) {

            throw new UnsupportedOperationException();
        }

        @Override
        public ConnectionDefinition getConnectionDefinition(String componentName, int componentVersion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ConnectionDefinition> getConnectionDefinitions() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ConnectionDefinition> getConnectionDefinitions(String componentName, Integer componentVersion) {
            throw new UnsupportedOperationException();
        }
    }
}
