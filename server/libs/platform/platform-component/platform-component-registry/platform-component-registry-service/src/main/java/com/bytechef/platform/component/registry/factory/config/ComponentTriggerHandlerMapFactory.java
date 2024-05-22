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

import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.platform.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.registry.handler.ComponentTriggerHandler;
import com.bytechef.platform.component.registry.handler.loader.ComponentHandlerLoader;
import com.bytechef.platform.component.registry.util.BeanUtils;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.platform.workflow.worker.trigger.factory.TriggerHandlerMapFactory;
import com.bytechef.platform.workflow.worker.trigger.handler.TriggerHandler;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public record ComponentTriggerHandlerMapFactory(
    List<ComponentHandlerLoader.ComponentHandlerEntry> componentHandlerEntries,
    InstanceAccessorRegistry instanceAccessorRegistry, TriggerDefinitionFacade triggerDefinitionFacade)
    implements TriggerHandlerMapFactory {

    @Override
    public Map<String, TriggerHandler> getTriggerHandlerMap() {
        return componentHandlerEntries.stream()
            .map(ComponentHandlerLoader.ComponentHandlerEntry::componentHandler)
            .map(ComponentHandler::getDefinition)
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
                    (TriggerDefinition triggerDefinition) -> BeanUtils.getBeanName(
                        componentDefinition.getName(), componentDefinition.getVersion(),
                        triggerDefinition.getName()),
                    triggerDefinition -> new ComponentTriggerHandler(
                        componentDefinition.getName(), componentDefinition.getVersion(),
                        triggerDefinition.getName(), instanceAccessorRegistry, triggerDefinitionFacade)));
    }
}
