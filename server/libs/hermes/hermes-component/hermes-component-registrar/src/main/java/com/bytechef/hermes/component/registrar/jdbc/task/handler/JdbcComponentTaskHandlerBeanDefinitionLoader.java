
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

package com.bytechef.hermes.component.registrar.jdbc.task.handler;

import com.bytechef.hermes.component.JdbcComponentDefinitionFactory;
import com.bytechef.hermes.component.registrar.task.handler.ComponentTaskHandlerBeanDefinitionLoader;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.JdbcComponentDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class JdbcComponentTaskHandlerBeanDefinitionLoader implements ComponentTaskHandlerBeanDefinitionLoader {

    @Override
    public List<ComponentTaskHandlerBeanDefinition> loadComponentTaskHandlerBeanDefinitions() {
        List<ComponentTaskHandlerBeanDefinition> componentTaskHandlerFactories = new ArrayList<>();

        for (JdbcComponentDefinitionFactory jdbcComponentDefinitionFactory : ServiceLoader.load(
            JdbcComponentDefinitionFactory.class)) {

            JdbcComponentDefinition jdbcComponentDefinition = jdbcComponentDefinitionFactory
                .getJdbcComponentDefinition();

            JdbcComponentHandler jdbcComponentHandler = new JdbcComponentHandler(jdbcComponentDefinition);

            ComponentDefinition componentDefinition = jdbcComponentHandler.getDefinition();

            componentTaskHandlerFactories.add(
                new ComponentTaskHandlerBeanDefinition(
                    componentDefinition,
                    componentDefinition.getActions()
                        .stream()
                        .map(actionDefinition -> new TaskHandlerBeanDefinitionEntry(
                            actionDefinition.getName(),
                            getBeanDefinition(actionDefinition, jdbcComponentHandler)))
                        .toList()));
        }

        return componentTaskHandlerFactories;
    }

    private BeanDefinition getBeanDefinition(
        ActionDefinition actionDefinition, JdbcComponentHandler jdbcComponentHandler) {

        return BeanDefinitionBuilder.genericBeanDefinition(JdbcComponentTaskHandler.class)
            .addConstructorArgValue(actionDefinition)
            .addConstructorArgReference("connectionDefinitionFacade")
            .addConstructorArgReference("connectionService")
            .addConstructorArgReference("dataSourceFactory")
            .addConstructorArgReference("eventPublisher")
            .addConstructorArgReference("fileStorageService")
            .addConstructorArgValue(jdbcComponentHandler)
            .getBeanDefinition();
    }

}
