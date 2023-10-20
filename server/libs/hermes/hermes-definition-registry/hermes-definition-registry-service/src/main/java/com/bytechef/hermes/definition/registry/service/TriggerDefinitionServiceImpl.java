
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

package com.bytechef.hermes.definition.registry.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public class TriggerDefinitionServiceImpl implements TriggerDefinitionService {

    private final List<ComponentDefinition> componentDefinitions;

    @SuppressFBWarnings("EI2")
    public TriggerDefinitionServiceImpl(List<ComponentDefinition> componentDefinitions) {
        this.componentDefinitions = componentDefinitions;
    }

    @Override
    public Mono<TriggerDefinitionDTO> getComponentTriggerDefinitionMono(
        String componentName, int componentVersion, String triggerName) {

        return Mono.just(
            componentDefinitions.stream()
                .filter(componentDefinition -> triggerName.equalsIgnoreCase(componentDefinition.getName()) &&
                    componentVersion == componentDefinition.getVersion())
                .flatMap(componentDefinition -> CollectionUtils.stream(componentDefinition.getTriggers()))
                .filter(triggerDefinition -> triggerName.equalsIgnoreCase(triggerDefinition.getName()))
                .findFirst()
                .map(this::toTriggerDefinitionDTO)
                .orElseThrow(IllegalArgumentException::new));
    }

    @Override
    public Mono<List<TriggerDefinitionDTO>> getComponentTriggerDefinitions(
        String componentName, int componentVersion) {
        return Mono.just(
            componentDefinitions.stream()
                .filter(componentDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                    componentVersion == componentDefinition.getVersion())
                .filter(componentDefinition -> componentDefinition.getTriggers() != null)
                .flatMap(componentDefinition -> CollectionUtils.stream(componentDefinition.getTriggers()))
                .map(this::toTriggerDefinitionDTO)
                .toList());
    }

    private TriggerDefinitionDTO toTriggerDefinitionDTO(TriggerDefinition triggerDefinition) {
        return new TriggerDefinitionDTO(
            triggerDefinition.getBatch(), triggerDefinition.getDisplay(), triggerDefinition.getExampleOutput(),
            triggerDefinition.getName(), triggerDefinition.getOutputSchema(), triggerDefinition.getProperties(),
            triggerDefinition.getResources(), triggerDefinition.getType());
    }
}
