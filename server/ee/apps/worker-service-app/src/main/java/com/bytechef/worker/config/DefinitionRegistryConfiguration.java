
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
import com.bytechef.hermes.component.registry.domain.ConnectionDefinition;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.component.registry.remote.client.service.ConnectionDefinitionServiceClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
public class DefinitionRegistryConfiguration {

    @Bean("workerConnectionDefinitionService")
    @Primary
    ConnectionDefinitionService connectionDefinitionService(
        @Qualifier("connectionDefinitionService") ConnectionDefinitionService connectionDefinitionService,
        ConnectionDefinitionServiceClient connectionDefinitionServiceClient) {

        return new WorkerConnectionDefinitionService(connectionDefinitionService, connectionDefinitionServiceClient);
    }

    /**
     * Compound ConnectionDefinitionService impl that supports the use case where a component (for example, HttpClient
     * or Script) uses a compatible connection from a different component that can be in a different worker instance.
     */
    private static class WorkerConnectionDefinitionService implements ConnectionDefinitionService {

        private final ConnectionDefinitionService connectionDefinitionService;
        private final ConnectionDefinitionServiceClient connectionDefinitionServiceClient;

        public WorkerConnectionDefinitionService(
            ConnectionDefinitionService connectionDefinitionService,
            ConnectionDefinitionServiceClient connectionDefinitionServiceClient) {

            this.connectionDefinitionService = connectionDefinitionService;
            this.connectionDefinitionServiceClient = connectionDefinitionServiceClient;
        }

        @Override
        public boolean connectionExists(String componentName, int connectionVersion) {
            return connectionDefinitionService.connectionExists(componentName, connectionVersion);
        }

        /**
         * Called from the Context.Connection instance.
         */
        @Override
        public ApplyResponse executeAuthorizationApply(
            String componentName, int connectionVersion, Map<String, ?> connectionParameters,
            String authorizationName) {

            if (connectionDefinitionService.connectionExists(componentName, connectionVersion)) {
                return connectionDefinitionService.executeAuthorizationApply(
                    componentName, connectionVersion, connectionParameters, authorizationName);
            } else {
                return connectionDefinitionServiceClient.executeAuthorizationApply(
                    componentName, connectionVersion, connectionParameters, authorizationName);
            }
        }

        /**
         * Called from the ConnectionFacade instance.
         */
        @Override
        public Authorization.AuthorizationCallbackResponse executeAuthorizationCallback(
            String componentName, int connectionVersion, Map<String, ?> connectionParameters,
            String authorizationName, String redirectUri) {

            return connectionDefinitionService.executeAuthorizationCallback(
                componentName, connectionVersion, connectionParameters, authorizationName, redirectUri);
        }

        /**
         * Called from the Context.Connection instance.
         */
        @Override
        public Optional<String> executeBaseUri(
            String componentName, int connectionVersion, Map<String, ?> connectionParameters) {
            if (connectionDefinitionService.connectionExists(componentName, connectionVersion)) {
                return connectionDefinitionService.executeBaseUri(
                    componentName, connectionVersion, connectionParameters);
            } else {
                return connectionDefinitionServiceClient.executeBaseUri(
                    componentName, connectionVersion, connectionParameters);
            }
        }

        @Override
        public Authorization.AuthorizationType getAuthorizationType(
            String authorizationName, String componentName, int connectionVersion) {

            return connectionDefinitionService.getAuthorizationType(
                authorizationName, componentName, connectionVersion);
        }

        @Override
        public ConnectionDefinition getConnectionDefinition(String componentName, int componentVersion) {
            return connectionDefinitionService.getConnectionDefinition(componentName, componentVersion);
        }

        @Override
        public List<ConnectionDefinition> getConnectionDefinitions(String componentName, Integer componentVersion) {
            return connectionDefinitionService.getConnectionDefinitions(componentName, componentVersion);
        }

        @Override
        public List<ConnectionDefinition> getConnectionDefinitions() {
            return connectionDefinitionService.getConnectionDefinitions();
        }

        @Override
        public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
            String componentName, int connectionVersion, Map<String, ?> connectionParameters,
            String authorizationName) {

            return connectionDefinitionService.getOAuth2AuthorizationParameters(
                componentName, connectionVersion, connectionParameters, authorizationName);
        }
    }
}
