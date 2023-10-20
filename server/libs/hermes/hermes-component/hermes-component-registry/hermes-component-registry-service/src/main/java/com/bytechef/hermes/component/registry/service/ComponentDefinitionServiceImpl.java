
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

package com.bytechef.hermes.component.registry.service;

import com.bytechef.hermes.component.registry.ComponentDefinitionRegistry;
import com.bytechef.hermes.component.registry.domain.ComponentDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

import java.util.List;

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
        com.bytechef.hermes.component.definition.ComponentDefinition componentDefinition =
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
    public List<ComponentDefinition> getComponentDefinitionVersions(String name) {
        return componentDefinitionRegistry.getComponentDefinitions(name)
            .stream()
            .map(ComponentDefinition::new)
            .toList();
    }
}
