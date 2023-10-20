
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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.registry.ComponentDefinitionRegistryImpl;
import com.bytechef.hermes.component.registrar.oas.handler.OpenApiComponentHandlerBeanDefinitionLoader;
import com.bytechef.hermes.component.registrar.handler.ComponentHandlerBeanDefinitionLoader;
import com.bytechef.hermes.component.registrar.handler.ComponentHandlerBeanDefinitionLoader.HandlerBeanDefinitionEntry;
import com.bytechef.hermes.component.registrar.handler.ComponentHandlerBeanDefinitionLoader.ComponentHandlerBeanDefinition;
import com.bytechef.hermes.component.registrar.jdbc.handler.JdbcComponentHandlerBeanDefinitionLoader;
import com.bytechef.hermes.component.registrar.handler.DefaultComponentHandlerBeanDefinitionLoader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@Component
public class ComponentHandlerBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private static final List<ComponentHandlerBeanDefinitionLoader> COMPONENT_HANDLER_BEAN_DEFINITION_LOADERS =
        List.of(
            new DefaultComponentHandlerBeanDefinitionLoader(),
            new JdbcComponentHandlerBeanDefinitionLoader(),
            new OpenApiComponentHandlerBeanDefinitionLoader());

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        List<ComponentHandlerBeanDefinition> componentHandlerFactories = COMPONENT_HANDLER_BEAN_DEFINITION_LOADERS
            .stream()
            .flatMap(
                componentHandlerBeanDefinitionLoader -> CollectionUtils.stream(
                    componentHandlerBeanDefinitionLoader.loadComponentHandlerBeanDefinitions()))
            .toList();

        BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;
        List<ComponentDefinition> componentDefinitions = new ArrayList<>();

        for (ComponentHandlerBeanDefinition componentHandlerBeanDefinition : componentHandlerFactories) {
            ComponentDefinitionFactory componentDefinitionFactory =
                componentHandlerBeanDefinition.componentDefinitionFactory();

            ComponentDefinition componentDefinition = componentDefinitionFactory.getDefinition();

            componentDefinitions.add(componentDefinition);

            for (HandlerBeanDefinitionEntry handlerBeanDefinitionEntry : componentHandlerBeanDefinition
                .handlerBeanDefinitionEntries()) {

                BeanDefinition handlerBeanDefinition = handlerBeanDefinitionEntry.handlerBeanDefinition();

                beanDefinitionRegistry.registerBeanDefinition(
                    getBeanName(
                        componentDefinition.getName(), componentDefinition.getVersion(),
                        handlerBeanDefinitionEntry.handlerName(), '/'),
                    handlerBeanDefinition);
            }
        }

        ManagedList<BeanDefinition> componentDefinitionFactoryManagedList = new ManagedList<>();

        Arrays.stream(beanFactory.getBeanNamesForType(ComponentDefinitionFactory.class))
            .map(beanFactory::getBeanDefinition)
            .forEach(componentDefinitionFactoryManagedList::add);

        beanDefinitionRegistry.registerBeanDefinition(
            "componentDefinitionRegistry",
            getComponentDefinitionRegistryBeanDefinition(
                componentDefinitions, componentDefinitionFactoryManagedList));
    }

    private String getBeanName(String componentName, int componentVersion, String typeName, char delimiter) {
        return componentName + delimiter + "v" + componentVersion + delimiter + typeName;
    }

    private BeanDefinition getComponentDefinitionRegistryBeanDefinition(
        List<ComponentDefinition> componentDefinitions,
        ManagedList<BeanDefinition> componentDefinitionFactoryManagedList) {

        return BeanDefinitionBuilder.genericBeanDefinition(ComponentDefinitionRegistryImpl.class)
            .addConstructorArgValue(componentDefinitions)
            .addConstructorArgValue(componentDefinitionFactoryManagedList)
            .getBeanDefinition();
    }
}
