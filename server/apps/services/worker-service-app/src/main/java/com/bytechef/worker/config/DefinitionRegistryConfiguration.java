
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

import com.bytechef.event.EventPublisher;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationContext;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.component.factory.ContextFactory;
import com.bytechef.hermes.definition.registry.component.factory.ContextFactoryImpl;
import com.bytechef.hermes.definition.registry.component.factory.InputParametersFactory;
import com.bytechef.hermes.definition.registry.component.factory.InputParametersFactoryImpl;
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
import com.bytechef.hermes.definition.registry.component.factory.ContextConnectionFactory;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
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
    List<ComponentDefinition> componentDefinitions(List<ComponentDefinitionFactory> componentDefinitionFactories) {
        return componentDefinitionFactories.stream()
            .map(ComponentDefinitionFactory::getDefinition)
            .sorted((o1, o2) -> {
                String o1Name = o1.getName();

                return o1Name.compareTo(o2.getName());
            })
            .toList();
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
    ConnectionDefinitionService connectionDefinitionService(
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
    ContextConnectionFactory contextConnectionFactory(
        ComponentDefinitionService componentDefinitionService,
        ConnectionDefinitionService connectionDefinitionService) {

        return new ContextConnectionFactory(componentDefinitionService, connectionDefinitionService);
    }

    @Bean
    ContextFactory contextFactory(
        ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService,
        EventPublisher eventPublisher, FileStorageService fileStorageService) {

        return new ContextFactoryImpl(
            connectionDefinitionService, connectionService, eventPublisher, fileStorageService);
    }

    @Bean
    InputParametersFactory inputParametersFactory() {
        return new InputParametersFactoryImpl();
    }

    @Bean
    TriggerDefinitionService triggerDefinitionService(
        List<ComponentDefinition> componentDefinitions, ContextConnectionFactory contextConnectionFactory) {

        return new TriggerDefinitionServiceImpl(componentDefinitions, contextConnectionFactory);
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
         */
        @Override
        public void executeAuthorizationApply(
            String componentName, int connectionVersion, Map<String, Object> connectionParameters,
            String authorizationName, AuthorizationContext authorizationContext) {

            if (connectionDefinitionService.connectionExists(componentName, connectionVersion)) {
                connectionDefinitionService.executeAuthorizationApply(
                    componentName, connectionVersion, connectionParameters, authorizationName, authorizationContext);
            } else {
                connectionDefinitionServiceRSocketClient.executeAuthorizationApply(
                    componentName, connectionVersion, connectionParameters, authorizationName, authorizationContext);
            }
        }

        /**
         * Called from the ConnectionFacade instance.
         */
        @Override
        public Authorization.AuthorizationCallbackResponse executeAuthorizationCallback(
            String componentName, int connectionVersion, Map<String, Object> connectionParameters,
            String authorizationName, String redirectUri) {

            return connectionDefinitionService.executeAuthorizationCallback(
                componentName, connectionVersion, connectionParameters, authorizationName, redirectUri);
        }

        /**
         * Called from the Context.Connection instance.
         */
        @Override
        public Optional<String> fetchBaseUri(
            String componentName, int connectionVersion, Map<String, Object> connectionParameters) {
            if (connectionDefinitionService.connectionExists(componentName, connectionVersion)) {
                return connectionDefinitionService.fetchBaseUri(
                    componentName, connectionVersion, connectionParameters);
            } else {
                return connectionDefinitionServiceRSocketClient.fetchBaseUri(
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
        public Mono<ConnectionDefinitionDTO> getConnectionDefinitionMono(
            String componentName, int componentVersion) {

            return connectionDefinitionService.getConnectionDefinitionMono(
                componentName, componentVersion);
        }

        @Override
        public Mono<List<ConnectionDefinitionDTO>> getConnectionDefinitionsMono(
            String componentName, int version) {

            return connectionDefinitionService.getConnectionDefinitionsMono(componentName, version);
        }

        @Override
        public Mono<List<ConnectionDefinitionDTO>> getConnectionDefinitionsMono() {
            return connectionDefinitionService.getConnectionDefinitionsMono();
        }

        @Override
        public OAuth2AuthorizationParametersDTO getOAuth2Parameters(
            String componentName, int connectionVersion, Map<String, Object> connectionParameters,
            String authorizationName) {
            return connectionDefinitionService.getOAuth2Parameters(
                componentName, connectionVersion, connectionParameters, authorizationName);
        }
    }
}
