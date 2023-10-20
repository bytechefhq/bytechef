
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

import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.impl.ComponentDefinitionFacadeImpl;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.service.impl.ComponentDefinitionServiceImpl;
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
    ComponentDefinitionFacade componentDefinitionFacade(
        ConnectionService connectionService, ComponentDefinitionService componentDefinitionService) {

        return new ComponentDefinitionFacadeImpl(connectionService, componentDefinitionService);
    }

    @Bean
    ComponentDefinitionService componentDefinitionService(
        List<ComponentDefinitionFactory> componentDefinitionFactories) {

        return new ComponentDefinitionServiceImpl(componentDefinitionFactories);
    }
}
