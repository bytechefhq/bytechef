
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

package com.bytechef.hermes.component.registrar.factory.config;

import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.registrar.oas.task.handler.OpenApiComponentTaskHandlerBeanDefinitionLoader;
import com.bytechef.hermes.component.registrar.task.handler.ComponentTaskHandlerBeanDefinitionLoader;
import com.bytechef.hermes.component.registrar.task.handler.ComponentTaskHandlerBeanDefinitionLoader.TaskHandlerBeanDefinitionEntry;
import com.bytechef.hermes.component.registrar.task.handler.ComponentTaskHandlerBeanDefinitionLoader.ComponentTaskHandlerBeanDefinition;
import com.bytechef.hermes.component.registrar.jdbc.task.handler.JdbcComponentTaskHandlerBeanDefinitionLoader;
import com.bytechef.hermes.component.registrar.task.handler.DefaultComponentTaskHandlerBeanDefinitionLoader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Component
public class ComponentTaskHandlerBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private List<ComponentTaskHandlerBeanDefinitionLoader> componentTaskHandlerBeanDefinitionLoaders = List.of(
        new DefaultComponentTaskHandlerBeanDefinitionLoader(),
        new JdbcComponentTaskHandlerBeanDefinitionLoader(),
        new OpenApiComponentTaskHandlerBeanDefinitionLoader());

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        List<ComponentTaskHandlerBeanDefinition> componentTaskHandlerFactories = componentTaskHandlerBeanDefinitionLoaders
            .stream()
            .flatMap(componentTaskHandlerBeanDefinitionLoader -> componentTaskHandlerBeanDefinitionLoader
                .loadComponentTaskHandlerBeanDefinitions()
                .stream())
            .toList();

        for (ComponentTaskHandlerBeanDefinition componentTaskHandlerBeanDefinition : componentTaskHandlerFactories) {
            ComponentDefinition componentDefinition = componentTaskHandlerBeanDefinition.componentDefinition();

            beanFactory.registerSingleton(
                getBeanName(componentDefinition.getName(), componentDefinition.getVersion(), "ComponentDefinition"),
                componentDefinition);
        }

        for (ComponentTaskHandlerBeanDefinition componentTaskHandlerBeanDefinition : componentTaskHandlerFactories) {
            ComponentDefinition componentDefinition = componentTaskHandlerBeanDefinition.componentDefinition();

            for (TaskHandlerBeanDefinitionEntry taskHandlerBeanDefinitionEntry : componentTaskHandlerBeanDefinition
                .taskHandlerBeanDefinitionEntries()) {

                BeanDefinition taskHandlerBeanDefinition = taskHandlerBeanDefinitionEntry.taskHandlerBeanDefinition();

                ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition(
                    getBeanName(
                        componentDefinition.getName(), componentDefinition.getVersion(),
                        taskHandlerBeanDefinitionEntry.actionDefinitionName()),
                    taskHandlerBeanDefinition);
            }
        }
    }

    private String getBeanName(String componentName, int version, String typeName) {
        return componentName + "/v" + version + "/" + typeName;
    }
}
