
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
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.data.storage.service.DataStorageService;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistry;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistryImpl;
import com.bytechef.hermes.definition.registry.component.factory.ContextFactory;
import com.bytechef.hermes.definition.registry.component.factory.ContextFactoryImpl;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OAuth2AuthorizationParametersDTO;
import com.bytechef.hermes.definition.registry.facade.ActionDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.ActionDefinitionFacadeImpl;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacadeImpl;
import com.bytechef.hermes.definition.registry.facade.TriggerDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.TriggerDefinitionFacadeImpl;
import com.bytechef.hermes.definition.registry.service.web.rest.client.facade.ComponentDefinitionFacadeClient;
import com.bytechef.hermes.definition.registry.service.web.rest.client.service.ActionDefinitionServiceClient;
import com.bytechef.hermes.definition.registry.service.web.rest.client.service.ComponentDefinitionServiceClient;
import com.bytechef.hermes.definition.registry.service.web.rest.client.service.ConnectionDefinitionServiceClient;
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
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
public class DefinitionRegistryConfiguration {

    @Bean
    ActionDefinitionFacade actionDefinitionFacade(
        ActionDefinitionService actionDefinitionService, ConnectionService connectionService) {

        return new ActionDefinitionFacadeImpl(actionDefinitionService, connectionService);
    }

    @Bean
    ActionDefinitionService actionDefinitionService(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextConnectionFactory contextConnectionFactory) {

        return new ActionDefinitionServiceImpl(componentDefinitionRegistry, contextConnectionFactory);
    }

    @Bean
    ActionDefinitionServiceClient actionDefinitionServiceClient(
        DiscoveryClient discoveryClient) {

        return new ActionDefinitionServiceClient(discoveryClient);
    }

    @Bean
    ComponentDefinitionFacade componentDefinitionFacade(
        ComponentDefinitionService componentDefinitionService, ConnectionService connectionService) {

        return new ComponentDefinitionFacadeImpl(componentDefinitionService, connectionService);
    }

    @Bean
    ComponentDefinitionFacadeClient componentDefinitionFacadeClient(DiscoveryClient discoveryClient) {
        return new ComponentDefinitionFacadeClient(discoveryClient);
    }

    @Bean
    public ComponentDefinitionRegistry componentDefinitionRegistry(
        List<ComponentDefinitionFactory> componentDefinitionFactories) {

        return new ComponentDefinitionRegistryImpl(componentDefinitionFactories);
    }

    @Bean
    ComponentDefinitionService componentDefinitionService(ComponentDefinitionRegistry componentDefinitionRegistry) {
        return new ComponentDefinitionServiceImpl(componentDefinitionRegistry);
    }

    @Bean
    ComponentDefinitionServiceClient componentDefinitionServiceClient(
        DiscoveryClient discoveryClient) {

        return new ComponentDefinitionServiceClient(discoveryClient);
    }

    @Bean
    ConnectionDefinitionService connectionDefinitionService(
        ComponentDefinitionRegistry componentDefinitionRegistry,
        ConnectionDefinitionServiceClient connectionDefinitionServiceClient) {

        return new WorkerConnectionDefinitionService(
            new ConnectionDefinitionServiceImpl(componentDefinitionRegistry), connectionDefinitionServiceClient);
    }

    @Bean
    ConnectionDefinitionServiceClient connectionDefinitionServiceClient(
        DiscoveryClient discoveryClient) {

        return new ConnectionDefinitionServiceClient(discoveryClient);
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
        DataStorageService dataStorageService, EventPublisher eventPublisher, FileStorageService fileStorageService) {

        return new ContextFactoryImpl(
            connectionDefinitionService, connectionService, dataStorageService, eventPublisher, fileStorageService);
    }

    @Bean
    TriggerDefinitionFacade triggerDefinitionFacade(
        ConnectionService connectionService, TriggerDefinitionService triggerDefinitionService) {

        return new TriggerDefinitionFacadeImpl(connectionService, triggerDefinitionService);
    }

    @Bean
    TriggerDefinitionService triggerDefinitionService(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextConnectionFactory contextConnectionFactory) {

        return new TriggerDefinitionServiceImpl(componentDefinitionRegistry, contextConnectionFactory);
    }

    /**
     * Compound ConnectionDefinitionService impl that supports the use case where a component (for example, HttpClient)
     * uses a connection from a different component that can be in a different worker instance.
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
        public void executeAuthorizationApply(
            String componentName, int connectionVersion, Map<String, ?> connectionParameters,
            String authorizationName, AuthorizationContext authorizationContext) {

            if (connectionDefinitionService.connectionExists(componentName, connectionVersion)) {
                connectionDefinitionService.executeAuthorizationApply(
                    componentName, connectionVersion, connectionParameters, authorizationName, authorizationContext);
            } else {
                connectionDefinitionServiceClient.executeAuthorizationApply(
                    componentName, connectionVersion, connectionParameters, authorizationName, authorizationContext);
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
        public Optional<String> fetchBaseUri(
            String componentName, int connectionVersion, Map<String, ?> connectionParameters) {
            if (connectionDefinitionService.connectionExists(componentName, connectionVersion)) {
                return connectionDefinitionService.fetchBaseUri(
                    componentName, connectionVersion, connectionParameters);
            } else {
                return connectionDefinitionServiceClient.fetchBaseUri(
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
        public ConnectionDefinitionDTO getConnectionDefinition(String componentName, int componentVersion) {
            return connectionDefinitionService.getConnectionDefinition(componentName, componentVersion);
        }

        @Override
        public List<ConnectionDefinitionDTO> getConnectionDefinitions(String componentName, int componentVersion) {
            return connectionDefinitionService.getConnectionDefinitions(componentName, componentVersion);
        }

        @Override
        public List<ConnectionDefinitionDTO> getConnectionDefinitions() {
            return connectionDefinitionService.getConnectionDefinitions();
        }

        @Override
        public OAuth2AuthorizationParametersDTO getOAuth2Parameters(
            String componentName, int connectionVersion, Map<String, ?> connectionParameters,
            String authorizationName) {

            return connectionDefinitionService.getOAuth2Parameters(
                componentName, connectionVersion, connectionParameters, authorizationName);
        }
    }
}
