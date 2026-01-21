/*
 * Copyright 2025 ByteChef
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

import static com.bytechef.commons.util.MemoizationUtils.memoize;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.handler.DynamicComponentHandlerRegistry;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader.ComponentHandlerEntry;
import com.bytechef.platform.component.handler.loader.DefaultComponentHandlerLoader;
import com.bytechef.platform.component.jdbc.handler.loader.JdbcComponentHandlerLoader;
import com.bytechef.platform.component.oas.handler.loader.OpenApiComponentHandlerLoader;
import com.bytechef.platform.component.task.handler.ComponentTaskHandlerProvider;
import com.bytechef.platform.component.trigger.handler.ComponentTriggerHandlerProvider;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ComponentHandlerBeanFactoryPostProcessor
    implements BeanFactoryPostProcessor, ApplicationListener<ContextRefreshedEvent> {

    private static final List<ComponentHandlerLoader> COMPONENT_HANDLER_LOADERS =
        List.of(
            new DefaultComponentHandlerLoader(), new JdbcComponentHandlerLoader(), new OpenApiComponentHandlerLoader());

    private static final Supplier<List<ComponentHandlerEntry>> COMPONENT_HANDLER_ENTRIES_SUPPLIER = memoize(
        () -> CollectionUtils.flatMap(COMPONENT_HANDLER_LOADERS, ComponentHandlerLoader::loadComponentHandlers));

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;

        beanDefinitionRegistry.registerBeanDefinition(
            "componentDefinitionRegistry",
            BeanDefinitionBuilder.genericBeanDefinition(
                ComponentDefinitionRegistry.class,
                () -> new ComponentDefinitionRegistry(
                    beanFactory.getBean(ApplicationProperties.class), List.copyOf(getComponentHandler(beanFactory)),
                    COMPONENT_HANDLER_ENTRIES_SUPPLIER, List.copyOf(getDynamicComponentHandlerRegistry(beanFactory))))
                .setLazyInit(true)
                .getBeanDefinition());

        beanDefinitionRegistry.registerBeanDefinition(
            "componentTaskHandlerProvider",
            BeanDefinitionBuilder.genericBeanDefinition(
                ComponentTaskHandlerProvider.class,
                () -> new ComponentTaskHandlerProvider(
                    COMPONENT_HANDLER_ENTRIES_SUPPLIER,
                    beanFactory.getBean("actionDefinitionFacade", ActionDefinitionFacade.class)))
                .setLazyInit(true)
                .getBeanDefinition());

        beanDefinitionRegistry.registerBeanDefinition(
            "componentTriggerHandlerProvider",
            BeanDefinitionBuilder.genericBeanDefinition(
                ComponentTriggerHandlerProvider.class,
                () -> new ComponentTriggerHandlerProvider(
                    List.copyOf(getComponentHandler(beanFactory)),
                    COMPONENT_HANDLER_ENTRIES_SUPPLIER,
                    beanFactory.getBean("triggerDefinitionFacade", TriggerDefinitionFacade.class)))
                .setLazyInit(true)
                .getBeanDefinition());
    }

    @Override
    @Async
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Trigger async load of component handlers on application startup
        COMPONENT_HANDLER_ENTRIES_SUPPLIER.get();
    }

    @NonNull
    private static Collection<ComponentHandler> getComponentHandler(ConfigurableListableBeanFactory beanFactory) {
        Map<String, ComponentHandler> beansOfType = beanFactory.getBeansOfType(ComponentHandler.class);

        return beansOfType.values();
    }

    @NonNull
    private static Collection<DynamicComponentHandlerRegistry> getDynamicComponentHandlerRegistry(
        ConfigurableListableBeanFactory beanFactory) {

        Map<String, DynamicComponentHandlerRegistry> beansOfType = beanFactory.getBeansOfType(
            DynamicComponentHandlerRegistry.class);

        return beansOfType.values();
    }
}
