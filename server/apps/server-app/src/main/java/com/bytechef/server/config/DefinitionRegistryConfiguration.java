
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

package com.bytechef.server.config;

import com.bytechef.event.EventPublisher;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.data.storage.service.DataStorageService;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistry;
import com.bytechef.hermes.definition.registry.facade.ActionDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.ActionDefinitionFacadeImpl;
import com.bytechef.hermes.definition.registry.facade.TriggerDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.TriggerDefinitionFacadeImpl;
import com.bytechef.hermes.definition.registry.task.dispatcher.TaskDispatcherDefinitionRegistry;
import com.bytechef.hermes.definition.registry.component.factory.ContextFactory;
import com.bytechef.hermes.definition.registry.component.factory.ContextFactoryImpl;
import com.bytechef.hermes.definition.registry.component.factory.InputParametersFactory;
import com.bytechef.hermes.definition.registry.component.factory.InputParametersFactoryImpl;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacadeImpl;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionServiceImpl;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionServiceImpl;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionServiceImpl;
import com.bytechef.hermes.definition.registry.service.TaskDispatcherDefinitionService;
import com.bytechef.hermes.definition.registry.service.TaskDispatcherDefinitionServiceImpl;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionServiceImpl;
import com.bytechef.hermes.definition.registry.component.factory.ContextConnectionFactory;
import com.bytechef.hermes.definition.registry.task.dispatcher.TaskDispatcherDefinitionRegistryImpl;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.bytechef.hermes.task.dispatcher.TaskDispatcherDefinitionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Ivica Cardic
 */
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
    ComponentDefinitionFacade componentDefinitionFacade(
        ComponentDefinitionService componentDefinitionService, ConnectionService connectionService) {

        return new ComponentDefinitionFacadeImpl(componentDefinitionService, connectionService);
    }

    @Bean
    ComponentDefinitionService componentDefinitionService(ComponentDefinitionRegistry componentDefinitionRegistry) {
        return new ComponentDefinitionServiceImpl(componentDefinitionRegistry);
    }

    @Bean
    ConnectionDefinitionService connectionDefinitionService(ComponentDefinitionRegistry componentDefinitionRegistry) {
        return new ConnectionDefinitionServiceImpl(componentDefinitionRegistry);
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
    InputParametersFactory inputParametersFactory() {
        return new InputParametersFactoryImpl();
    }

    @Bean
    public TaskDispatcherDefinitionRegistry taskDispatcherRegistry(
        List<TaskDispatcherDefinitionFactory> taskDispatcherDefinitionFactories) {

        return new TaskDispatcherDefinitionRegistryImpl(taskDispatcherDefinitionFactories);
    }

    @Bean
    TriggerDefinitionFacade triggerDefinitionFacade(
        ConnectionService connectionService, TriggerDefinitionService triggerDefinitionService) {

        return new TriggerDefinitionFacadeImpl(connectionService, triggerDefinitionService);
    }

    @Bean
    TaskDispatcherDefinitionService
        taskDispatcherDefinitionService(TaskDispatcherDefinitionRegistry taskDispatcherDefinitionRegistry) {
        return new TaskDispatcherDefinitionServiceImpl(taskDispatcherDefinitionRegistry);
    }

    @Bean
    TriggerDefinitionService triggerDefinitionService(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextConnectionFactory contextConnectionFactory) {

        return new TriggerDefinitionServiceImpl(componentDefinitionRegistry, contextConnectionFactory);
    }
}
