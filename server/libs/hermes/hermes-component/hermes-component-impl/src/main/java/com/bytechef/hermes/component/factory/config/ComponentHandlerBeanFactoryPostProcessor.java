
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

package com.bytechef.hermes.component.factory.config;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.oas.loader.OpenAPIComponentDefinitionFactoryBeanDefinitionLoader;
import com.bytechef.hermes.component.loader.ComponentDefinitionFactoryBeanDefinitionLoader;
import com.bytechef.hermes.component.loader.ComponentDefinitionFactoryBeanDefinitionLoader.HandlerBeanDefinitionEntry;
import com.bytechef.hermes.component.loader.ComponentDefinitionFactoryBeanDefinitionLoader.ComponentDefinitionFactoryBeanDefinition;
import com.bytechef.hermes.component.jdbc.loader.JdbcComponentDefinitionFactoryBeanDefinitionLoader;
import com.bytechef.hermes.component.loader.DefaultComponentDefinitionFactoryBeanDefinitionLoader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ComponentHandlerBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private static final List<ComponentDefinitionFactoryBeanDefinitionLoader> COMPONENT_DEFINITION_FACTORY__BEAN_DEFINITION_LOADERS =
        List.of(
            new DefaultComponentDefinitionFactoryBeanDefinitionLoader(),
            new JdbcComponentDefinitionFactoryBeanDefinitionLoader(),
            new OpenAPIComponentDefinitionFactoryBeanDefinitionLoader());

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        List<ComponentDefinitionFactoryBeanDefinition> componentDefinitionFactoryBeanDefinitions =
            COMPONENT_DEFINITION_FACTORY__BEAN_DEFINITION_LOADERS.stream()
                .flatMap(
                    componentDefinitionFactoryBeanDefinitionLoader -> CollectionUtils.stream(
                        componentDefinitionFactoryBeanDefinitionLoader.loadComponentDefinitionFactoryBeanDefinitions()))
                .toList();

        for (ComponentDefinitionFactoryBeanDefinition componentDefinitionFactoryBeanDefinition : componentDefinitionFactoryBeanDefinitions) {

            ComponentDefinitionFactory componentDefinitionFactory =
                componentDefinitionFactoryBeanDefinition.componentDefinitionFactory();

            beanFactory.registerSingleton(
                getBeanName(
                    componentDefinitionFactory.getName(), componentDefinitionFactory.getVersion(),
                    "ComponentDefinitionFactory", '_'),
                componentDefinitionFactory);

            BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;

            for (HandlerBeanDefinitionEntry handlerBeanDefinitionEntry : componentDefinitionFactoryBeanDefinition
                .handlerBeanDefinitionEntries()) {

                BeanDefinition handlerBeanDefinition = handlerBeanDefinitionEntry.handlerBeanDefinition();

                beanDefinitionRegistry.registerBeanDefinition(
                    getBeanName(
                        componentDefinitionFactory.getName(), componentDefinitionFactory.getVersion(),
                        handlerBeanDefinitionEntry.handlerName(), '/'),
                    handlerBeanDefinition);
            }
        }
    }

    private String getBeanName(String componentName, int componentVersion, String typeName, char delimiter) {
        return componentName + delimiter + "v" + componentVersion + delimiter + typeName;
    }
}
