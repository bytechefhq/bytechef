
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

package com.bytechef.hermes.component.registrar.task.handler;

import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractComponentTaskHandlerBeanDefinitionLoader<T extends ComponentDefinitionFactory>
    implements ComponentTaskHandlerBeanDefinitionLoader {

    private final Class<T> componentDefinitionFactoryClass;

    protected AbstractComponentTaskHandlerBeanDefinitionLoader(Class<T> componentDefinitionFactoryClass) {
        this.componentDefinitionFactoryClass = componentDefinitionFactoryClass;
    }

    public List<ComponentTaskHandlerBeanDefinition> loadComponentTaskHandlerBeanDefinitions() {
        List<ComponentTaskHandlerBeanDefinition> componentTaskHandlerFactories = new ArrayList<>();

        for (T componentDefinitionFactory : ServiceLoader.load(componentDefinitionFactoryClass)) {
            ComponentDefinition componentDefinition = componentDefinitionFactory.getDefinition();

            List<TaskHandlerBeanDefinitionEntry> taskHandlerBeanDefinitionEntries = new ArrayList<>();

            componentDefinition
                .getActions()
                .ifPresent(
                    actionDefinitions -> taskHandlerBeanDefinitionEntries.addAll(
                        com.bytechef.commons.util.CollectionUtils.map(
                            actionDefinitions,
                            actionDefinition -> new TaskHandlerBeanDefinitionEntry(
                                actionDefinition.getName(),
                                getComponentActionTaskHandlerBeanDefinition(
                                    actionDefinition, componentDefinitionFactory)))));

            componentDefinition
                .getTriggers()
                .ifPresent(triggerDefinitions -> {
                    for (TriggerDefinition triggerDefinition : triggerDefinitions) {
                        BeanDefinition triggeBeanDefinition = getComponentTriggerTaskHandlerBeanDefinition(
                            triggerDefinition, componentDefinitionFactory);

                        if (triggeBeanDefinition != null) {
                            taskHandlerBeanDefinitionEntries.add(
                                new TaskHandlerBeanDefinitionEntry(
                                    triggerDefinition.getName(),
                                    getComponentTriggerTaskHandlerBeanDefinition(
                                        triggerDefinition, componentDefinitionFactory)));
                        }
                    }
                });

            componentTaskHandlerFactories.add(
                new ComponentTaskHandlerBeanDefinition(componentDefinition, taskHandlerBeanDefinitionEntries));
        }

        return componentTaskHandlerFactories;
    }

    protected abstract BeanDefinition getComponentActionTaskHandlerBeanDefinition(
        ActionDefinition actionDefinition, T componentDefinitionFactory);

    protected BeanDefinition getComponentTriggerTaskHandlerBeanDefinition(
        TriggerDefinition triggerDefinition, T componentDefinitionFactory) {

        BeanDefinition beanDefinition = null;

        TriggerType triggerType = triggerDefinition.getType();

        if (triggerType != TriggerType.LISTENER) {
            beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(DefaultComponentTriggerTaskHandler.class)
                .addConstructorArgValue(componentDefinitionFactory)
                .addConstructorArgReference("connectionDefinitionService")
                .addConstructorArgReference("connectionService")
                .addConstructorArgReference("eventPublisher")
                .addConstructorArgReference("fileStorageService")
                .addConstructorArgValue(triggerDefinition)
                .getBeanDefinition();
        }

        return beanDefinition;
    }
}
