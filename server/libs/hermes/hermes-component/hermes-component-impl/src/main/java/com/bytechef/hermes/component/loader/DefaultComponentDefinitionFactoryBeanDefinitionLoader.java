
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

package com.bytechef.hermes.component.loader;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.ComponentHandler.ActionHandlerFunction;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ActionDefinitionWrapper;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinitionWrapper;
import com.bytechef.hermes.component.definition.ComponentHandlerWrapper;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.handler.DefaultComponentActionTaskHandler;
import com.bytechef.hermes.component.handler.DefaultComponentTriggerHandler;
import com.bytechef.hermes.definition.registry.component.util.CustomActionUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * @author Ivica Cardic
 */
public class DefaultComponentDefinitionFactoryBeanDefinitionLoader
    implements ComponentDefinitionFactoryBeanDefinitionLoader {

    public List<ComponentDefinitionFactoryBeanDefinition> loadComponentDefinitionFactoryBeanDefinitions() {
        List<ComponentDefinitionFactoryBeanDefinition> componentDefinitionFactoryBeanDefinitions = new ArrayList<>();

        for (ComponentHandler componentHandler : ServiceLoader.load(ComponentHandler.class)) {
            ComponentDefinition componentDefinition = componentHandler.getDefinition();

            List<? extends ActionDefinition> actionDefinitions = OptionalUtils.mapOrElse(
                componentDefinition.getActions(),
                curActionDefinitions -> CollectionUtils.map(
                    curActionDefinitions,
                    actionDefinition -> {
                        if (OptionalUtils.isPresent(componentHandler.getActionHandler())) {
                            return new DefaultActionDefinitionWrapper(
                                actionDefinition, OptionalUtils.get(componentHandler.getActionHandler()));
                        } else {
                            return actionDefinition;
                        }
                    }),
                List.of());

            ComponentHandler componentHandlerWrapper = new ComponentHandlerWrapper(
                new DefaultComponentDefinitionWrapper(componentDefinition, actionDefinitions));

            List<HandlerBeanDefinitionEntry> handlerBeanDefinitionEntries = new ArrayList<>(
                CollectionUtils.map(actionDefinitions,
                    actionDefinition -> new HandlerBeanDefinitionEntry(
                        actionDefinition.getName(),
                        getComponentActionTaskHandlerBeanDefinition(
                            actionDefinition.getName(), componentHandler.getName(), componentHandler.getVersion()))));

            // Custom Actions support

            if (OptionalUtils.orElse(componentDefinition.getCustomAction(), false)) {
                handlerBeanDefinitionEntries.add(
                    new HandlerBeanDefinitionEntry(
                        CustomActionUtils.CUSTOM,
                        getComponentActionTaskHandlerBeanDefinition(
                            CustomActionUtils.CUSTOM, componentHandler.getName(), componentHandler.getVersion())));
            }

            List<? extends TriggerDefinition> triggerDefinitions = OptionalUtils.orElse(
                componentDefinition.getTriggers(), Collections.emptyList());

            for (TriggerDefinition triggerDefinition : triggerDefinitions) {
                BeanDefinition triggeBeanDefinition = getComponentTriggerHandlerBeanDefinition(
                    componentDefinition.getName(), componentDefinition.getVersion(), triggerDefinition.getName(),
                    triggerDefinition.getType());

                if (triggeBeanDefinition == null) {
                    continue;
                }

                handlerBeanDefinitionEntries.add(
                    new HandlerBeanDefinitionEntry(
                        triggerDefinition.getName(),
                        getComponentTriggerHandlerBeanDefinition(
                            componentDefinition.getName(), componentDefinition.getVersion(),
                            triggerDefinition.getName(), triggerDefinition.getType())));
            }

            componentDefinitionFactoryBeanDefinitions.add(
                new ComponentDefinitionFactoryBeanDefinition(componentHandlerWrapper, handlerBeanDefinitionEntries));
        }

        return componentDefinitionFactoryBeanDefinitions;
    }

    private BeanDefinition getComponentActionTaskHandlerBeanDefinition(
        String actionName, String componentName, int componentVersion) {

        return BeanDefinitionBuilder.genericBeanDefinition(DefaultComponentActionTaskHandler.class)
            .addConstructorArgValue(componentName)
            .addConstructorArgValue(componentVersion)
            .addConstructorArgValue(actionName)
            .addConstructorArgReference("actionDefinitionService")
            .getBeanDefinition();
    }

    private BeanDefinition getComponentTriggerHandlerBeanDefinition(
        String componentName, int componentVersion, String triggerName, TriggerDefinition.TriggerType triggerType) {

        BeanDefinition beanDefinition = null;

        if (triggerType != TriggerDefinition.TriggerType.LISTENER) {
            beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(DefaultComponentTriggerHandler.class)
                .addConstructorArgValue(componentName)
                .addConstructorArgValue(componentVersion)
                .addConstructorArgValue(triggerName)
                .addConstructorArgReference("triggerDefinitionService")
                .getBeanDefinition();
        }

        return beanDefinition;
    }

    private static class DefaultActionDefinitionWrapper extends ActionDefinitionWrapper {

        private final ActionHandlerFunction actionHandlerFunction;

        public DefaultActionDefinitionWrapper(
            ActionDefinition actionDefinition, ActionHandlerFunction actionHandlerFunction) {

            super(actionDefinition);

            this.actionHandlerFunction = actionHandlerFunction;
        }

        @Override
        public Optional<PerformFunction> getPerform() {
            return Optional.of(
                (inputParameters, context) -> actionHandlerFunction.apply(name, inputParameters, context));
        }
    }

    private static class DefaultComponentDefinitionWrapper extends ComponentDefinitionWrapper {

        private final List<? extends ActionDefinition> actionDefinitions;

        public DefaultComponentDefinitionWrapper(
            ComponentDefinition componentDefinition, List<? extends ActionDefinition> actionDefinitions) {

            super(componentDefinition);

            this.actionDefinitions = actionDefinitions;
        }

        @Override
        public Optional<List<? extends ActionDefinition>> getActions() {
            return Optional.of(actionDefinitions);
        }
    }
}
