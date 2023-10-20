
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

package com.bytechef.hermes.component.registrar.handler;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.definition.registry.component.action.CustomAction;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractComponentHandlerBeanDefinitionLoader<T extends ComponentDefinitionFactory>
    implements ComponentHandlerBeanDefinitionLoader {

    private final Class<T> componentDefinitionFactoryClass;

    protected AbstractComponentHandlerBeanDefinitionLoader(Class<T> componentDefinitionFactoryClass) {
        this.componentDefinitionFactoryClass = componentDefinitionFactoryClass;
    }

    public List<ComponentHandlerBeanDefinition> loadComponentHandlerBeanDefinitions() {
        List<ComponentHandlerBeanDefinition> componentHandlerFactories = new ArrayList<>();

        for (T componentDefinitionFactory : ServiceLoader.load(componentDefinitionFactoryClass)) {
            ComponentDefinition componentDefinition = componentDefinitionFactory.getDefinition();
            List<HandlerBeanDefinitionEntry> handlerBeanDefinitionEntries = new ArrayList<>();

            List<? extends ActionDefinition> actionDefinitions = OptionalUtils.orElse(
                componentDefinition.getActions(), Collections.emptyList());

            handlerBeanDefinitionEntries.addAll(
                com.bytechef.commons.util.CollectionUtils.map(
                    actionDefinitions,
                    actionDefinition -> new HandlerBeanDefinitionEntry(
                        actionDefinition.getName(),
                        getComponentActionTaskHandlerBeanDefinition(
                            actionDefinition, componentDefinitionFactory))));

            // Custom Actions support

            if (OptionalUtils.orElse(componentDefinition.getCustomAction(), false)) {
                handlerBeanDefinitionEntries.add(
                    new HandlerBeanDefinitionEntry(
                        CustomAction.CUSTOM,
                        getComponentActionTaskHandlerBeanDefinition(
                            CustomAction.getCustomActionDefinition(componentDefinition),
                            componentDefinitionFactory)));
            }

            List<? extends TriggerDefinition> triggerDefinitions = OptionalUtils.orElse(
                componentDefinition.getTriggers(), Collections.emptyList());

            for (TriggerDefinition triggerDefinition : triggerDefinitions) {
                BeanDefinition triggeBeanDefinition = getComponentTriggerHandlerBeanDefinition(
                    triggerDefinition, componentDefinitionFactory);

                if (triggeBeanDefinition == null) {
                    continue;
                }

                handlerBeanDefinitionEntries.add(
                    new HandlerBeanDefinitionEntry(
                        triggerDefinition.getName(),
                        getComponentTriggerHandlerBeanDefinition(triggerDefinition, componentDefinitionFactory)));
            }

            componentHandlerFactories.add(
                new ComponentHandlerBeanDefinition(componentDefinitionFactory, handlerBeanDefinitionEntries));
        }

        return componentHandlerFactories;
    }

    protected abstract BeanDefinition getComponentActionTaskHandlerBeanDefinition(
        ActionDefinition actionDefinition, T componentDefinitionFactory);

    protected BeanDefinition getComponentTriggerHandlerBeanDefinition(
        TriggerDefinition triggerDefinition, T componentDefinitionFactory) {

        BeanDefinition beanDefinition = null;
        TriggerType triggerType = triggerDefinition.getType();

        if (triggerType != TriggerType.LISTENER) {
            beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(DefaultComponentTriggerHandler.class)
                .addConstructorArgValue(componentDefinitionFactory)
                .addConstructorArgReference("contextFactory")
                .addConstructorArgReference("dataStorageService")
                .addConstructorArgReference("inputParametersFactory")
                .addConstructorArgValue(triggerDefinition)
                .getBeanDefinition();
        }

        return beanDefinition;
    }
}
