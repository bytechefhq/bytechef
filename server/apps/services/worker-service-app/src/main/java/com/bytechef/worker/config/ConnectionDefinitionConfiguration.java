
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
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import com.bytechef.hermes.definition.registry.rsocket.client.service.ConnectionDefinitionServiceRSocketClient;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.definition.registry.service.LocalConnectionDefinitionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Configuration
public class ConnectionDefinitionConfiguration {

    @Bean
    @Primary
    ConnectionDefinitionService connectionDefinitionServiceChain(
        ConnectionDefinitionServiceRSocketClient connectionDefinitionServiceRSocketClient,
        LocalConnectionDefinitionService localConnectionDefinitionService) {

        return new WorkerConnectionDefinitionService(
            localConnectionDefinitionService, connectionDefinitionServiceRSocketClient);
    }

    /**
     * Compound ConnectionDefinitionService impl that supports the use case where a component (for example, HttpClient)
     * uses a connection from a different component that can be in a different worker instance.
     */
    private static class WorkerConnectionDefinitionService implements ConnectionDefinitionService {

        private final LocalConnectionDefinitionService localConnectionDefinitionService;
        private final ConnectionDefinitionServiceRSocketClient connectionDefinitionServiceRSocketClient;

        public WorkerConnectionDefinitionService(
            LocalConnectionDefinitionService localConnectionDefinitionService,
            ConnectionDefinitionServiceRSocketClient connectionDefinitionServiceRSocketClient) {

            this.localConnectionDefinitionService = localConnectionDefinitionService;
            this.connectionDefinitionServiceRSocketClient = connectionDefinitionServiceRSocketClient;
        }

        /**
         * Called from the Context.Connection instance.
         *
         * @param connection
         * @param authorizationContext
         */
        @Override
        public void executeAuthorizationApply(
            Connection connection, Authorization.AuthorizationContext authorizationContext) {

            if (localConnectionDefinitionService.connectionExists(
                connection.getComponentName(), connection.getConnectionVersion())) {

                localConnectionDefinitionService.executeAuthorizationApply(connection, authorizationContext);
            } else {
                connectionDefinitionServiceRSocketClient.executeAuthorizationApply(connection, authorizationContext);
            }
        }

        /**
         * Called from the ConnectionFacade instance.
         *
         * @param connection
         * @param redirectUri
         * @return
         */
        @Override
        public Authorization.AuthorizationCallbackResponse executeAuthorizationCallback(
            Connection connection, String redirectUri) {

            return localConnectionDefinitionService.executeAuthorizationCallback(connection, redirectUri);
        }

        /**
         * Called from the Context.Connection instance.
         *
         * @param connection
         * @return
         */
        @Override
        public Optional<String> fetchBaseUri(Connection connection) {
            if (localConnectionDefinitionService.connectionExists(
                connection.getComponentName(), connection.getConnectionVersion())) {

                return localConnectionDefinitionService.fetchBaseUri(connection);
            } else {
                return connectionDefinitionServiceRSocketClient.fetchBaseUri(connection);
            }
        }

        @Override
        public Authorization.AuthorizationType getAuthorizationType(
            String authorizationName, String componentName, int connectionVersion) {

            return localConnectionDefinitionService.getAuthorizationType(
                authorizationName, componentName, connectionVersion);
        }

        @Override
        public Mono<ConnectionDefinition> getComponentConnectionDefinitionMono(
            String componentName, int componentVersion) {

            return localConnectionDefinitionService.getComponentConnectionDefinitionMono(
                componentName, componentVersion);
        }

        @Override
        public Mono<List<ConnectionDefinition>> getComponentConnectionDefinitionsMono(
            String componentName, int version) {

            return localConnectionDefinitionService.getComponentConnectionDefinitionsMono(componentName, version);
        }

        @Override
        public Mono<List<ConnectionDefinition>> getConnectionDefinitionsMono() {
            return localConnectionDefinitionService.getConnectionDefinitionsMono();
        }

        @Override
        public OAuth2AuthorizationParametersDTO getOAuth2Parameters(Connection connection) {
            return localConnectionDefinitionService.getOAuth2Parameters(connection);
        }
    }
}
