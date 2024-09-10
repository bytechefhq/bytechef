/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.handler.factory;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.component.handler.ComponentHandlerServiceLoaderFactory;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader;
import com.bytechef.platform.component.handler.loader.DefaultComponentHandlerLoader;
import com.bytechef.platform.component.jdbc.handler.loader.JdbcComponentHandlerLoader;
import com.bytechef.platform.component.oas.handler.loader.OpenAPIComponentHandlerLoader;
import com.bytechef.platform.component.task.handler.ComponentTaskHandlerFactory;
import com.bytechef.platform.component.trigger.handler.ComponentTriggerHandlerFactory;
import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ComponentHandlerBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private static final List<ComponentHandlerLoader> COMPONENT_HANDLER_LOADERS =
        List.of(
            new DefaultComponentHandlerLoader(), new JdbcComponentHandlerLoader(), new OpenAPIComponentHandlerLoader());

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        List<? extends ComponentHandlerLoader.ComponentHandlerEntry> componentHandlerEntries =
            CollectionUtils.flatMap(COMPONENT_HANDLER_LOADERS, ComponentHandlerLoader::loadComponentHandlers);

        beanFactory.registerSingleton(
            "componentHandlerServiceLoaderFactory", new ComponentHandlerServiceLoaderFactory(
                CollectionUtils.map(
                    componentHandlerEntries, ComponentHandlerLoader.ComponentHandlerEntry::componentHandler)));

        BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;

        beanDefinitionRegistry.registerBeanDefinition(
            "componentTaskHandlerMapFactory",
            BeanDefinitionBuilder.genericBeanDefinition(ComponentTaskHandlerFactory.class)
                .addConstructorArgValue(componentHandlerEntries)
                .addConstructorArgReference("actionDefinitionFacade")
                .getBeanDefinition());

        beanDefinitionRegistry.registerBeanDefinition(
            "componentTriggerHandlerMapFactory",
            BeanDefinitionBuilder.genericBeanDefinition(ComponentTriggerHandlerFactory.class)
                .addConstructorArgValue(componentHandlerEntries)
                .addConstructorArgReference("instanceAccessorRegistry")
                .addConstructorArgReference("triggerDefinitionFacade")
                .getBeanDefinition());
    }
}
