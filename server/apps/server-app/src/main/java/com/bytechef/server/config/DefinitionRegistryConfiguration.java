
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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.connection.service.ConnectionService;
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
    ActionDefinitionService actionDefinitionService(List<ComponentDefinition> componentDefinitions) {
        return new ActionDefinitionServiceImpl(componentDefinitions);
    }

    @Bean
    ComponentDefinitionFacade componentDefinitionFacade(
        ComponentDefinitionService componentDefinitionService, ConnectionService connectionService) {

        return new ComponentDefinitionFacadeImpl(componentDefinitionService, connectionService);
    }

    @Bean
    List<ComponentDefinition> componentDefinitions(List<ComponentDefinitionFactory> componentDefinitionFactories) {
        return CollectionUtils.map(componentDefinitionFactories, ComponentDefinitionFactory::getDefinition);
    }

    @Bean
    ComponentDefinitionService componentDefinitionService(List<ComponentDefinition> componentDefinitions) {
        return new ComponentDefinitionServiceImpl(componentDefinitions);
    }

    @Bean
    ConnectionDefinitionService connectionDefinitionService(List<ComponentDefinition> componentDefinitions) {
        return new ConnectionDefinitionServiceImpl(componentDefinitions);
    }

    @Bean
    TaskDispatcherDefinitionService taskDispatcherDefinitionService(
        List<TaskDispatcherDefinitionFactory> taskDispatcherDefinitionFactories) {

        return new TaskDispatcherDefinitionServiceImpl(
            taskDispatcherDefinitionFactories
                .stream()
                .map(TaskDispatcherDefinitionFactory::getDefinition)
                .toList());
    }

    @Bean
    TriggerDefinitionService triggerDefinitionService(List<ComponentDefinition> componentDefinitions) {
        return new TriggerDefinitionServiceImpl(componentDefinitions);
    }
}
