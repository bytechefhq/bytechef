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

package com.bytechef.hermes.component.registry.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.registry.domain.ComponentDefinition;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class ComponentDefinitionFacadeImpl implements ComponentDefinitionFacade {

    private final ComponentDefinitionService componentDefinitionService;

    @SuppressFBWarnings("EI")
    public ComponentDefinitionFacadeImpl(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean triggerDefinitions, List<String> include) {

        List<ComponentDefinition> components = componentDefinitionService.getComponentDefinitions()
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

    private static Predicate<ComponentDefinition> filter(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean triggerDefinitions, List<String> include) {

        return componentDefinition -> {
            if (include != null && !include.isEmpty() && !include.contains(componentDefinition.getName())) {
                return false;
            }

            if (actionDefinitions != null && CollectionUtils.isEmpty(componentDefinition.getActions())) {
                return false;
            }

            if (connectionDefinitions != null && !OptionalUtils.isPresent(componentDefinition.getConnection())) {
                return false;
            }

            if (triggerDefinitions != null && CollectionUtils.isEmpty(componentDefinition.getTriggers())) {
                return false;
            }

            return true;
        };
    }
}
