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

package com.bytechef.platform.component.registry.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.component.registry.ComponentDefinitionRegistry;
import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service("componentDefinitionService")
public class ComponentDefinitionServiceImpl implements ComponentDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;

    @SuppressFBWarnings("EI2")
    public ComponentDefinitionServiceImpl(ComponentDefinitionRegistry componentDefinitionRegistry) {
        this.componentDefinitionRegistry = componentDefinitionRegistry;
    }

    @Override
    public ComponentDefinition getComponentDefinition(String name, Integer version) {
        com.bytechef.component.definition.ComponentDefinition componentDefinition =
            componentDefinitionRegistry.getComponentDefinition(name, version);

        return new ComponentDefinition(componentDefinition);
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions() {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .map(ComponentDefinition::new)
            .toList();
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean triggerDefinitions, List<String> include) {

        List<ComponentDefinition> components = getComponentDefinitions()
            .stream()
            .filter(filter(actionDefinitions, connectionDefinitions, triggerDefinitions, include))
            .distinct()
            .toList();

        if (include != null && !include.isEmpty()) {
            components = new ArrayList<>(components);

            components.sort(Comparator.comparing(component -> include.indexOf(component.getName())));
        }

        return components;
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitionVersions(String name) {
        return componentDefinitionRegistry.getComponentDefinitions(name)
            .stream()
            .map(ComponentDefinition::new)
            .toList();
    }

    private static Predicate<ComponentDefinition> filter(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean triggerDefinitions, List<String> include) {

        return componentDefinition -> {
            if (include != null && include.contains(componentDefinition.getName())) {
                return true;
            }

            if (actionDefinitions != null && !CollectionUtils.isEmpty(componentDefinition.getActions())) {
                return true;
            }

            if (connectionDefinitions != null && componentDefinition.getConnection() != null) {
                return true;
            }

            if (triggerDefinitions != null && !CollectionUtils.isEmpty(componentDefinition.getTriggers())) {
                return true;
            }

            if (include == null && actionDefinitions == null && connectionDefinitions == null &&
                triggerDefinitions == null) {

                return true;
            }

            return false;
        };
    }
}
