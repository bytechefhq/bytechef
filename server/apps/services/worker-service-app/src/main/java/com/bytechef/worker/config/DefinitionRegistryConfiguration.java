
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

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacadeImpl;
import com.bytechef.hermes.definition.registry.rsocket.client.facade.ComponentDefinitionFacadeRSocketClient;
import com.bytechef.hermes.definition.registry.rsocket.client.service.ActionDefinitionServiceRSocketClient;
import com.bytechef.hermes.definition.registry.rsocket.client.service.ComponentDefinitionServiceRSocketClient;
import com.bytechef.hermes.definition.registry.rsocket.client.service.ConnectionDefinitionServiceRSocketClient;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionServiceImpl;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionServiceImpl;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionServiceImpl;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Configuration
public class DefinitionRegistryConfiguration {

    @Bean
    ActionDefinitionService actionDefinitionService(List<ComponentDefinition> componentDefinitions) {
        return new ActionDefinitionServiceImpl(componentDefinitions);
    }

    @Bean
    ActionDefinitionServiceRSocketClient actionDefinitionServiceRSocketClient(
        DiscoveryClient discoveryClient,
        @Qualifier("workerRSocketRequesterBuilder") RSocketRequester.Builder rSocketRequesterBuilder) {

        return new ActionDefinitionServiceRSocketClient(discoveryClient, rSocketRequesterBuilder);
    }

    @Bean
    ComponentDefinitionFacade componentDefinitionFacade(
        ComponentDefinitionService componentDefinitionService, ConnectionService connectionService) {

        return new ComponentDefinitionFacadeImpl(componentDefinitionService, connectionService);
    }

    @Bean
    ComponentDefinitionFacadeRSocketClient componentDefinitionFacadeRSocketClient(
        DiscoveryClient discoveryClient,
        @Qualifier("workerRSocketRequesterBuilder") RSocketRequester.Builder rSocketRequesterBuilder) {
        return new ComponentDefinitionFacadeRSocketClient(discoveryClient, rSocketRequesterBuilder);
    }

    @Bean
    ComponentDefinitionService componentDefinitionService(List<ComponentDefinition> componentDefinitions) {
        return new ComponentDefinitionServiceImpl(componentDefinitions);
    }

    @Bean
    ComponentDefinitionServiceRSocketClient componentDefinitionServiceRSocketClient(
        DiscoveryClient discoveryClient,
        @Qualifier("workerRSocketRequesterBuilder") RSocketRequester.Builder rSocketRequesterBuilder) {

        return new ComponentDefinitionServiceRSocketClient(discoveryClient, rSocketRequesterBuilder);
    }

    @Bean
    @Primary
    WorkerConnectionDefinitionService connectionDefinitionService(
        List<ComponentDefinition> componentDefinitions,
        ConnectionDefinitionServiceRSocketClient connectionDefinitionServiceRSocketClient) {

        return new WorkerConnectionDefinitionService(
            new ConnectionDefinitionServiceImpl(componentDefinitions), connectionDefinitionServiceRSocketClient);
    }

    @Bean
    ConnectionDefinitionServiceRSocketClient connectionDefinitionServiceRSocketClient(
        DiscoveryClient discoveryClient,
        @Qualifier("workerRSocketRequesterBuilder") RSocketRequester.Builder rSocketRequesterBuilder) {

        return new ConnectionDefinitionServiceRSocketClient(discoveryClient, rSocketRequesterBuilder);
    }

    @Bean
    TriggerDefinitionService triggerDefinitionService(List<ComponentDefinition> componentDefinitions) {
        return new TriggerDefinitionServiceImpl(componentDefinitions);
    }

    /**
     * Compound ConnectionDefinitionService impl that supports the use case where a component (for example, HttpClient)
     * uses a connection from a different component that can be in a different worker instance.
     */
    private static class WorkerConnectionDefinitionService implements ConnectionDefinitionService {

        private final ConnectionDefinitionService connectionDefinitionService;
        private final ConnectionDefinitionServiceRSocketClient connectionDefinitionServiceRSocketClient;

        public WorkerConnectionDefinitionService(
            ConnectionDefinitionService connectionDefinitionService,
            ConnectionDefinitionServiceRSocketClient connectionDefinitionServiceRSocketClient) {

            this.connectionDefinitionService = connectionDefinitionService;
            this.connectionDefinitionServiceRSocketClient = connectionDefinitionServiceRSocketClient;
        }

        @Override
        public boolean connectionExists(String componentName, int connectionVersion) {
            return connectionDefinitionService.connectionExists(componentName, connectionVersion);
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

            if (connectionDefinitionService.connectionExists(
                connection.getComponentName(), connection.getConnectionVersion())) {

                connectionDefinitionService.executeAuthorizationApply(connection, authorizationContext);
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

            return connectionDefinitionService.executeAuthorizationCallback(connection, redirectUri);
        }

        /**
         * Called from the Context.Connection instance.
         *
         * @param connection
         * @return
         */
        @Override
        public Optional<String> fetchBaseUri(Connection connection) {
            if (connectionDefinitionService.connectionExists(
                connection.getComponentName(), connection.getConnectionVersion())) {

                return connectionDefinitionService.fetchBaseUri(connection);
            } else {
                return connectionDefinitionServiceRSocketClient.fetchBaseUri(connection);
            }
        }

        @Override
        public Authorization.AuthorizationType getAuthorizationType(
            String authorizationName, String componentName, int connectionVersion) {

            return connectionDefinitionService.getAuthorizationType(
                authorizationName, componentName, connectionVersion);
        }

        @Override
        public Mono<ConnectionDefinitionDTO> getComponentConnectionDefinitionMono(
            String componentName, int componentVersion) {

            return connectionDefinitionService.getComponentConnectionDefinitionMono(
                componentName, componentVersion);
        }

        @Override
        public Mono<List<ConnectionDefinitionDTO>> getComponentConnectionDefinitionsMono(
            String componentName, int version) {

            return connectionDefinitionService.getComponentConnectionDefinitionsMono(componentName, version);
        }

        @Override
        public Mono<List<ConnectionDefinitionDTO>> getConnectionDefinitionsMono() {
            return connectionDefinitionService.getConnectionDefinitionsMono();
        }

        @Override
        public OAuth2AuthorizationParametersDTO getOAuth2Parameters(Connection connection) {
            return connectionDefinitionService.getOAuth2Parameters(connection);
        }

        @Override
        public Context.Connection toContextConnection(Connection connection) {
            return connectionDefinitionService.toContextConnection(connection);
        }
    }
}
