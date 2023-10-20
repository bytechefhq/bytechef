
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

package com.bytechef.hermes.component.handler.loader;

import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinitionWrapper;
import com.bytechef.hermes.component.definition.ComponentHandlerWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.BiFunction;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractComponentHandlerLoader<T extends ComponentHandler> implements ComponentHandlerLoader {

    private final BiFunction<ComponentHandler, ActionDefinition, ActionDefinition> actionDefinitionMapper;
    private final Class<T> serviceClass;

    public AbstractComponentHandlerLoader(
        BiFunction<ComponentHandler, ActionDefinition, ActionDefinition> actionDefinitionMapper,
        Class<T> serviceClass) {

        this.actionDefinitionMapper = actionDefinitionMapper;
        this.serviceClass = serviceClass;
    }

    public List<ComponentHandlerEntry> loadComponentHandlers() {
        List<ComponentHandlerEntry> componentHandlerEntries = new ArrayList<>();

        for (T componentHandler : ServiceLoader.load(serviceClass)) {
            componentHandlerEntries.add(
                new ComponentHandlerEntry(
                    new ComponentHandlerWrapper(
                        new ComponentDefinitionWrapper(componentHandler, actionDefinitionMapper)),
                    getComponentTaskHandlerFunction(componentHandler)));
        }

        return componentHandlerEntries;
    }

    protected abstract ComponentTaskHandlerFunction getComponentTaskHandlerFunction(T componentHandler);
}
