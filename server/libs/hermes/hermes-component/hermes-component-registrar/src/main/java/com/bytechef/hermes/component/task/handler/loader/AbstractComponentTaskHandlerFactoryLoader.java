
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

package com.bytechef.hermes.component.task.handler.loader;

import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Ivica Cardic
 */
abstract class AbstractComponentTaskHandlerFactoryLoader<T extends ComponentDefinitionFactory>
    implements ComponentTaskHandlerFactoryLoader {

    private final Class<T> componentDefinitionFactoryClass;

    protected AbstractComponentTaskHandlerFactoryLoader(Class<T> componentDefinitionFactoryClass) {
        this.componentDefinitionFactoryClass = componentDefinitionFactoryClass;
    }

    public List<ComponentTaskHandlerFactory> loadComponentTaskHandlerFactories() {
        List<ComponentTaskHandlerFactory> componentTaskHandlerFactories = new ArrayList<>();

        for (T componentDefinitionFactory : ServiceLoader.load(componentDefinitionFactoryClass)) {
            ComponentDefinition componentDefinition = componentDefinitionFactory.getDefinition();

            componentTaskHandlerFactories.add(
                new ComponentTaskHandlerFactory(
                    componentDefinition,
                    componentDefinition.getActions()
                        .stream()
                        .map(actionDefinition -> new TaskHandlerFactoryEntry(
                            actionDefinition.getName(),
                            createTaskHandlerFactory(
                                actionDefinition, componentDefinition.getConnection(), componentDefinitionFactory)))
                        .toList()));
        }

        return componentTaskHandlerFactories;
    }

    protected abstract TaskHandlerFactory createTaskHandlerFactory(
        ActionDefinition actionDefinition, ConnectionDefinition connectionDefinition, T componentDefinitionFactory);

}
