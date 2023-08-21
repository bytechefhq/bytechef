
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

package com.bytechef.hermes.component.jdbc.loader;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.JdbcComponentDefinitionFactory;
import com.bytechef.hermes.component.handler.DefaultComponentActionTaskHandler;
import com.bytechef.hermes.component.jdbc.handler.JdbcComponentHandler;
import com.bytechef.hermes.component.loader.ComponentDefinitionFactoryBeanDefinitionLoader;
import com.bytechef.hermes.component.definition.ComponentDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * @author Ivica Cardic
 */
public class JdbcComponentDefinitionFactoryBeanDefinitionLoader
    implements ComponentDefinitionFactoryBeanDefinitionLoader {

    @Override
    public List<ComponentDefinitionFactoryBeanDefinition> loadComponentDefinitionFactoryBeanDefinitions() {
        List<ComponentDefinitionFactoryBeanDefinition> componentTaskHandlerFactories = new ArrayList<>();

        for (JdbcComponentDefinitionFactory jdbcComponentDefinitionFactory : ServiceLoader.load(
            JdbcComponentDefinitionFactory.class)) {

            JdbcComponentHandler jdbcComponentHandler = new JdbcComponentHandler(
                jdbcComponentDefinitionFactory.getJdbcComponentDefinition());

            ComponentDefinition componentDefinition = jdbcComponentHandler.getDefinition();

            componentTaskHandlerFactories.add(
                new ComponentDefinitionFactoryBeanDefinition(
                    jdbcComponentHandler,
                    CollectionUtils.map(
                        OptionalUtils.orElse(componentDefinition.getActions(), Collections.emptyList()),
                        actionDefinition -> new HandlerBeanDefinitionEntry(
                            actionDefinition.getName(),
                            getBeanDefinition(
                                componentDefinition.getName(), componentDefinition.getVersion(),
                                actionDefinition.getName())))));
        }

        return componentTaskHandlerFactories;
    }

    private BeanDefinition getBeanDefinition(String componentName, int componentVersion, String actionName) {
        return BeanDefinitionBuilder.genericBeanDefinition(DefaultComponentActionTaskHandler.class)
            .addConstructorArgValue(componentName)
            .addConstructorArgValue(componentVersion)
            .addConstructorArgValue(actionName)
            .addConstructorArgReference("actionDefinitionService")
            .getBeanDefinition();
    }

}
