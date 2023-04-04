
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
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.definition.registry.dto.ActionDefinitionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionServiceImpl implements ActionDefinitionService {

    private final List<ComponentDefinition> componentDefinitions;

    @SuppressFBWarnings("EI2")
    public ActionDefinitionServiceImpl(List<ComponentDefinition> componentDefinitions) {
        this.componentDefinitions = componentDefinitions;
    }

    @Override
    public Mono<ActionDefinitionDTO> getComponentActionDefinitionMono(
        String componentName, int componentVersion, String actionName) {

        return Mono.just(
            componentDefinitions.stream()
                .filter(componentDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                    componentVersion == componentDefinition.getVersion())
                .flatMap(componentDefinition -> CollectionUtils.stream(componentDefinition.getActions()))
                .filter(actionDefinition -> actionName.equalsIgnoreCase(actionDefinition.getName()))
                .map(actionDefinition -> (ActionDefinition) actionDefinition)
                .findFirst()
                .map(this::toActionDefinitionDTO)
                .orElseThrow(IllegalArgumentException::new));
    }

    @Override
    public Mono<List<ActionDefinitionDTO>> getComponentActionDefinitionsMono(
        String componentName, int componentVersion) {
        return Mono.just(
            componentDefinitions.stream()
                .filter(componentDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                    componentVersion == componentDefinition.getVersion())
                .flatMap(componentDefinition -> CollectionUtils.stream(componentDefinition.getActions()))
                .map(actionDefinition -> (ActionDefinition) actionDefinition)
                .map(this::toActionDefinitionDTO)
                .toList());
    }

    private ActionDefinitionDTO toActionDefinitionDTO(ActionDefinition actionDefinition) {
        return new ActionDefinitionDTO(
            actionDefinition.getBatch(), actionDefinition.getDisplay(), actionDefinition.getExampleOutput(),
            actionDefinition.getName(), actionDefinition.getOutputSchema(), actionDefinition.getProperties(),
            actionDefinition.getResources());
    }
}
