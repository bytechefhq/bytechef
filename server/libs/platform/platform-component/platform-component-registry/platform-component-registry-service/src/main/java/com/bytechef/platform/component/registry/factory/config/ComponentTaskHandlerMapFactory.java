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

package com.bytechef.platform.component.registry.factory.config;

import com.bytechef.atlas.worker.task.factory.TaskHandlerMapFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.registry.handler.loader.ComponentHandlerLoader;
import com.bytechef.platform.component.registry.util.BeanUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ComponentTaskHandlerMapFactory(
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
                            actionDefinition -> BeanUtils.getBeanName(
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
