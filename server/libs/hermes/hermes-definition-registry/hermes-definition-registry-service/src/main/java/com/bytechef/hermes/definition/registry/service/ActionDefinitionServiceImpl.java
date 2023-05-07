
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

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.registry.dto.ActionDefinitionDTO;
import com.bytechef.hermes.definition.registry.component.action.CustomAction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

        ComponentDefinition componentDefinition = componentDefinitions.stream()
            .filter(curComponentDefinition -> componentName.equalsIgnoreCase(curComponentDefinition.getName()) &&
                componentVersion == curComponentDefinition.getVersion())
            .findFirst()
            .orElseThrow();

        ActionDefinitionDTO actionDefinitionDTO;

        if (Objects.equals(actionName, CustomAction.CUSTOM)) {
            actionDefinitionDTO = toActionDefinitionDTO(CustomAction.getCustomActionDefinition(componentDefinition));
        } else {
            actionDefinitionDTO =
                OptionalUtils.orElse(componentDefinition.getActions(), Collections.emptyList())
                    .stream()
                    .filter(actionDefinition -> actionName.equalsIgnoreCase(actionDefinition.getName()))
                    .findFirst()
                    .map(this::toActionDefinitionDTO)
                    .orElseThrow(
                        () -> new IllegalArgumentException(
                            "The component '%s' does not contain the '%s' action.".formatted(
                                componentName, actionName)));
        }

        return Mono.just(actionDefinitionDTO);
    }

    @Override
    public Mono<List<ActionDefinitionDTO>> getComponentActionDefinitionsMono(
        String componentName, int componentVersion) {

        ComponentDefinition componentDefinition =
            componentDefinitions.stream()
                .filter(curComponentDefinition -> componentName.equalsIgnoreCase(curComponentDefinition.getName()) &&
                    componentVersion == curComponentDefinition.getVersion())
                .findFirst()
                .orElseThrow();

        List<ActionDefinitionDTO> actionDefinitionDTOs =
            OptionalUtils.orElse(componentDefinition.getActions(), Collections.emptyList())
                .stream()
                .map(this::toActionDefinitionDTO)
                .toList();

        if (OptionalUtils.orElse(componentDefinition.getCustomAction(), false)) {
            actionDefinitionDTOs = new ArrayList<>(actionDefinitionDTOs);

            actionDefinitionDTOs.add(
                toActionDefinitionDTO(CustomAction.getCustomActionDefinition(componentDefinition)));
        }

        return Mono.just(actionDefinitionDTOs);
    }

    private ActionDefinitionDTO toActionDefinitionDTO(ActionDefinition actionDefinition) {
        List<? extends Property<?>> outputSchema = Collections.emptyList();

        if (OptionalUtils.orElse(actionDefinition.getOutputSchemaProperty(), null) == null) {
            outputSchema = OptionalUtils.orElse(actionDefinition.getOutputSchema(), Collections.emptyList());
        } else {
            // TODO
            // Parse outputSchemaProperty and build new output schema definition.
            // Use SampleDataType to parse sample. Default is JSON.
        }

        return new ActionDefinitionDTO(
            OptionalUtils.orElse(actionDefinition.getBatch(), false), actionDefinition.getDescription(),
            OptionalUtils.orElse(actionDefinition.getExampleOutput(), null),
            OptionalUtils.orElse(actionDefinition.getHelp(), null), actionDefinition.getName(),
            outputSchema, OptionalUtils.orElse(actionDefinition.getProperties(), Collections.emptyList()),
            actionDefinition.getTitle());
    }
}
