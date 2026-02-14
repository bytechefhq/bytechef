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

package com.bytechef.platform.component.handler.loader;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.ComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ComponentHandlerWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.BiFunction;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractComponentHandlerLoader<T extends ComponentHandler> implements ComponentHandlerLoader {

    private final BiFunction<T, ActionDefinition, ActionDefinition> actionDefinitionMapperFunction;
    private final Class<T> serviceClass;

    public AbstractComponentHandlerLoader(
        BiFunction<T, ActionDefinition, ActionDefinition> actionDefinitionMapperFunction,
        Class<T> serviceClass) {

        this.actionDefinitionMapperFunction = actionDefinitionMapperFunction;
        this.serviceClass = serviceClass;
    }

    public List<ComponentHandlerEntry> loadComponentHandlers() {
        List<ComponentHandlerEntry> componentHandlerEntries = new ArrayList<>();

        for (T componentHandler : ServiceLoader.load(serviceClass)) {
            ComponentDefinition componentDefinition = componentHandler.getDefinition();

            componentHandlerEntries.add(
                new ComponentHandlerEntry(
                    new ComponentHandlerWrapper(
                        new ComponentDefinitionWrapper(
                            componentDefinition, mapActionDefinitions(componentHandler, componentDefinition))),
                    getComponentTaskHandlerFunction(componentHandler)));
        }

        return componentHandlerEntries;
    }

    protected abstract ComponentTaskHandlerFunction getComponentTaskHandlerFunction(T componentHandler);

    private List<ActionDefinition> mapActionDefinitions(T componentHandler, ComponentDefinition componentDefinition) {
        return componentDefinition.getActions()
            .map(
                actionDefinitions -> CollectionUtils.map(
                    actionDefinitions,
                    actionDefinition -> actionDefinitionMapperFunction.apply(componentHandler, actionDefinition)))
            .orElse(List.of());
    }
}
