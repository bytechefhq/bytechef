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

package com.bytechef.component.script.engine;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.polyglot.ComponentCatalog;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.ApplicationContext;

/**
 * {@link ComponentCatalog} adapter backing the script component's {@code context.component} proxy chain.
 * {@link ComponentDefinitionService} is resolved lazily from the {@link ApplicationContext} on every call, matching the
 * pre-extraction {@code PolyglotEngine} behavior.
 *
 * @author Ivica Cardic
 */
class ScriptComponentCatalog implements ComponentCatalog {

    private final ApplicationContext applicationContext;
    private final Map<String, ComponentDefinition> componentDefinitionMap = new ConcurrentHashMap<>();

    ScriptComponentCatalog(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean hasComponent(String name) {
        ComponentDefinitionService componentDefinitionService = applicationContext.getBean(
            ComponentDefinitionService.class);

        componentDefinitionMap.computeIfAbsent(
            name, key -> componentDefinitionService.getComponentDefinition(key, null));

        return componentDefinitionMap.containsKey(name);
    }

    @Override
    public boolean hasAction(String componentName, String actionName) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName);

        return componentDefinition.getActions()
            .stream()
            .anyMatch(actionDefinition -> Objects.equals(actionDefinition.getName(), actionName));
    }

    ComponentDefinition getComponentDefinition(String componentName) {
        ComponentDefinitionService componentDefinitionService = applicationContext.getBean(
            ComponentDefinitionService.class);

        return componentDefinitionMap.computeIfAbsent(
            componentName, key -> componentDefinitionService.getComponentDefinition(key, null));
    }

}
