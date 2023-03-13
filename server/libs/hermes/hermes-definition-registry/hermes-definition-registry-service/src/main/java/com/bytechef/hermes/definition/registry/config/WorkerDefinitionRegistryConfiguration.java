
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

package com.bytechef.hermes.definition.registry.config;

import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacadeImpl;
import com.bytechef.hermes.definition.registry.facade.ConnectionDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.ConnectionDefinitionFacadeImpl;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionServiceImpl;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionServiceImpl;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("'${spring.application.name}'=='server-app' or '${spring.application.name}'=='worker-service-app'")
public class WorkerDefinitionRegistryConfiguration {

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
    ComponentDefinitionService componentDefinitionService(List<ComponentDefinition> componentDefinitions) {
        return new ComponentDefinitionServiceImpl(componentDefinitions);
    }

    @Bean
    ConnectionDefinitionFacade connectionDefinitionFacade(
        ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService) {

        return new ConnectionDefinitionFacadeImpl(connectionDefinitionService, connectionService);
    }

    @Bean
    ConnectionDefinitionService connectionDefinitionService(List<ComponentDefinition> componentDefinitions) {
        return new ConnectionDefinitionServiceImpl(componentDefinitions);
    }
}
