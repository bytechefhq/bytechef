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

package com.bytechef.platform.component.factory.config;

import com.bytechef.atlas.worker.task.factory.TaskHandlerMapFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.ComponentDefinitionFactory;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.platform.component.handler.ComponentTriggerHandler;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader;
import com.bytechef.platform.component.handler.loader.DefaultComponentHandlerLoader;
import com.bytechef.platform.component.jdbc.handler.loader.JdbcComponentHandlerLoader;
import com.bytechef.platform.component.oas.handler.loader.OpenApiComponentHandlerLoader;
import com.bytechef.platform.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.registry.factory.ComponentHandlerListFactory;
import com.bytechef.platform.workflow.worker.trigger.factory.TriggerHandlerMapFactory;
import com.bytechef.platform.workflow.worker.trigger.handler.TriggerHandler;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
            new DefaultComponentHandlerLoader(), new JdbcComponentHandlerLoader(), new OpenApiComponentHandlerLoader());

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        List<? extends ComponentHandlerLoader.ComponentHandlerEntry> componentHandlerEntries =
            CollectionUtils.flatMap(COMPONENT_HANDLER_LOADERS, ComponentHandlerLoader::loadComponentHandlers);

        beanFactory.registerSingleton(
            "componentHandlerListFactory", new ComponentHandlerListFactoryImpl(
                CollectionUtils.map(
                    componentHandlerEntries, ComponentHandlerLoader.ComponentHandlerEntry::componentHandler)));

        BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;

        beanDefinitionRegistry.registerBeanDefinition(
            "componentTaskHandlerMapFactory",
            BeanDefinitionBuilder.genericBeanDefinition(ComponentTaskHandlerMapFactory.class)
                .addConstructorArgValue(componentHandlerEntries)
                .addConstructorArgReference("actionDefinitionFacade")
                .getBeanDefinition());

        beanDefinitionRegistry.registerBeanDefinition(
            "componentTriggerHandlerMapFactory",
            BeanDefinitionBuilder.genericBeanDefinition(ComponentTriggerHandlerMapFactory.class)
                .addConstructorArgValue(componentHandlerEntries)
                .addConstructorArgReference("triggerDefinitionFacade")
                .getBeanDefinition());
    }

    private static String getBeanName(String componentName, int componentVersion, String operationName) {
        return componentName + '/' + "v" + componentVersion + '/' + operationName;
    }

    private record ComponentTaskHandlerMapFactory(
        List<ComponentHandlerLoader.ComponentHandlerEntry> componentHandlerEntries,
        ActionDefinitionFacade actionDefinitionFacade)
        implements TaskHandlerMapFactory {

        @Override
        @SuppressWarnings({
            "rawtypes", "unchecked"
        })
        public Map<String, TaskHandler<?>> getTaskHandlerMap() {
            return (Map) componentHandlerEntries
                .stream()
                .map(componentHandlerEntry -> {
                    ComponentHandler componentHandler = componentHandlerEntry.componentHandler();

                    ComponentDefinition componentDefinition = componentHandler.getDefinition();

                    return OptionalUtils.orElse(componentDefinition.getActions(), List.of())
                        .stream()
                        .collect(
                            Collectors.toMap(
                                actionDefinition -> getBeanName(
                                    componentDefinition.getName(), componentDefinition.getVersion(),
                                    actionDefinition.getName()),
                                actionDefinition -> {
                                    ComponentHandlerLoader.ComponentTaskHandlerFunction componentTaskHandlerFunction =
                                        componentHandlerEntry.componentTaskHandlerFunction();

                                    return componentTaskHandlerFunction.apply(
                                        actionDefinition.getName(), actionDefinitionFacade);
                                }));
                })
                .reduce(Map.of(), MapUtils::concat);
        }
    }

    private record ComponentTriggerHandlerMapFactory(
        List<ComponentHandlerLoader.ComponentHandlerEntry> componentHandlerEntries,
        TriggerDefinitionFacade triggerDefinitionFacade)
        implements TriggerHandlerMapFactory {

        @Override
        public Map<String, TriggerHandler> getTriggerHandlerMap() {
            return componentHandlerEntries.stream()
                .map(ComponentHandlerLoader.ComponentHandlerEntry::componentHandler)
                .map(ComponentDefinitionFactory::getDefinition)
                .map(this::collect)
                .reduce(Map.of(), MapUtils::concat);
        }

        private Map<String, TriggerHandler> collect(ComponentDefinition componentDefinition) {
            return OptionalUtils.orElse(componentDefinition.getTriggers(), List.of())
                .stream()
                .filter(triggerDefinition -> triggerDefinition != null &&
                    triggerDefinition.getType() != TriggerDefinition.TriggerType.LISTENER)
                .collect(
                    Collectors.toMap(
                        (TriggerDefinition triggerDefinition) -> getBeanName(
                            componentDefinition.getName(), componentDefinition.getVersion(),
                            triggerDefinition.getName()),
                        triggerDefinition -> new ComponentTriggerHandler(
                            componentDefinition.getName(), componentDefinition.getVersion(),
                            triggerDefinition.getName(), triggerDefinitionFacade)));
        }
    }

    private record ComponentHandlerListFactoryImpl(List<? extends ComponentHandler> componentHandlers)
        implements ComponentHandlerListFactory {

        @Override
        public List<? extends ComponentHandler> getComponentHandlers() {
            return componentHandlers;
        }
    }
}
