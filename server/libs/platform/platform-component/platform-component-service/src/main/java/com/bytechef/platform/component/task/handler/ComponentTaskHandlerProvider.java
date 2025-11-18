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

package com.bytechef.platform.component.task.handler;

import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerProvider;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader;
import com.bytechef.platform.component.util.BeanUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public final class ComponentTaskHandlerProvider implements TaskHandlerProvider {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final Supplier<List<ComponentHandlerLoader.ComponentHandlerEntry>> componentHandlerEntriesSupplier;

    public ComponentTaskHandlerProvider(
        Supplier<List<ComponentHandlerLoader.ComponentHandlerEntry>> componentHandlerEntriesSupplier,
        ActionDefinitionFacade actionDefinitionFacade) {

        this.componentHandlerEntriesSupplier = componentHandlerEntriesSupplier;
        this.actionDefinitionFacade = actionDefinitionFacade;
    }

    @Override
    public Map<String, TaskHandler<?>> getTaskHandlerMap() {
        return componentHandlerEntriesSupplier.get()
            .stream()
            .map(componentHandlerEntry -> collect(componentHandlerEntry, actionDefinitionFacade))
            .reduce(Map.of(), MapUtils::concat);
    }

    private Map<String, TaskHandler<?>> collect(
        ComponentHandlerLoader.ComponentHandlerEntry componentHandlerEntry,
        ActionDefinitionFacade actionDefinitionFacade) {

        ComponentHandler componentHandler = componentHandlerEntry.componentHandler();

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        return OptionalUtils.orElse(componentDefinition.getActions(), List.of())
            .stream()
            .collect(
                Collectors.toMap(
                    actionDefinition -> BeanUtils.getBeanName(
                        componentDefinition.getName(), componentDefinition.getVersion(),
                        actionDefinition.getName()),
                    actionDefinition -> {
                        ComponentHandlerLoader.ComponentTaskHandlerFunction componentTaskHandlerFunction =
                            componentHandlerEntry.componentTaskHandlerFunction();

                        return componentTaskHandlerFunction.apply(
                            actionDefinition.getName(), actionDefinitionFacade);
                    }));
    }
}
