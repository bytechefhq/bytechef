
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
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.definition.registry.dto.ActionDefinitionBasicDTO;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionBasicDTO;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionBasicDTO;
import com.bytechef.hermes.definition.registry.util.DefinitionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionServiceImpl implements ComponentDefinitionService {

    private static final ComponentDefinition MANUAL_COMPONENT_DEFINITION = component("manual")
        .title("Manual")
        .triggers(trigger("trigger"));

    private final List<ComponentDefinition> componentDefinitions;

    @SuppressFBWarnings("EI2")
    public ComponentDefinitionServiceImpl(List<ComponentDefinition> componentDefinitions) {
        this.componentDefinitions = CollectionUtils.concat(componentDefinitions, MANUAL_COMPONENT_DEFINITION)
            .stream()
            .sorted((o1, o2) -> {
                String o1Name = o1.getName();

                return o1Name.compareTo(o2.getName());
            })
            .toList();
    }

    @Override
    public ComponentDefinitionDTO getComponentDefinition(String name, Integer version) {
        ComponentDefinition componentDefinition;

        List<ComponentDefinition> filteredComponentDefinitions = componentDefinitions.stream()
            .filter(curComponentDefinition -> name.equalsIgnoreCase(curComponentDefinition.getName()))
            .toList();

        if (version == null) {
            componentDefinition = filteredComponentDefinitions.get(filteredComponentDefinitions.size() - 1);
        } else {
            componentDefinition = CollectionUtils.getFirst(
                filteredComponentDefinitions,
                curComponentDefinition -> version == curComponentDefinition.getVersion());
        }

        return toComponentDefinitionDTO(componentDefinition);
    }

    @Override
    public Mono<ComponentDefinitionDTO> getComponentDefinitionMono(String name, Integer version) {
        return Mono.just(getComponentDefinition(name, version));
    }

    @Override
    public Mono<List<ComponentDefinitionDTO>> getComponentDefinitionsMono() {
        return Mono.just(
            componentDefinitions.stream()
                .map(this::toComponentDefinitionDTO)
                .toList());
    }

    @Override
    public Mono<List<ComponentDefinitionDTO>> getComponentDefinitionsMono(String name) {
        return Mono.just(
            CollectionUtils.map(
                componentDefinitions.stream()
                    .filter(componentDefinition -> Objects.equals(componentDefinition.getName(), name))
                    .toList(),
                this::toComponentDefinitionDTO));
    }

    private ComponentDefinitionDTO toComponentDefinitionDTO(ComponentDefinition componentDefinition) {
        return new ComponentDefinitionDTO(
            OptionalUtils.mapOrElse(
                componentDefinition.getActions(),
                actionDefinitions -> CollectionUtils.map(actionDefinitions, this::toActionDefinitionBasicDTO),
                Collections.emptyList()),
            OptionalUtils.orElse(componentDefinition.getCategory(), null),
            OptionalUtils.mapOrElse(componentDefinition.getConnection(), this::toConnectionDefinitionDTO, null),
            OptionalUtils.orElse(componentDefinition.getDescription(), null),
            DefinitionUtils.readIcon(componentDefinition.getIcon()),
            componentDefinition.getName(), OptionalUtils.orElse(componentDefinition.getResources(), null),
            OptionalUtils.orElse(componentDefinition.getTags(), null),
            OptionalUtils.mapOrElse(
                componentDefinition.getTriggers(),
                triggerDefinitions -> CollectionUtils.map(triggerDefinitions, this::toTriggerDefinitionBasicDTO),
                Collections.emptyList()),
            componentDefinition.getTitle(), componentDefinition.getVersion());
    }

    private ActionDefinitionBasicDTO toActionDefinitionBasicDTO(ActionDefinition actionDefinition) {
        return new ActionDefinitionBasicDTO(
            OptionalUtils.orElse(actionDefinition.getBatch(), false), actionDefinition.getDescription(),
            OptionalUtils.orElse(actionDefinition.getHelp(), null), actionDefinition.getName(),
            actionDefinition.getTitle());
    }

    private ConnectionDefinitionBasicDTO toConnectionDefinitionDTO(ConnectionDefinition connectionDefinition) {
        return new ConnectionDefinitionBasicDTO(
            OptionalUtils.orElse(connectionDefinition.getDescription(), null),
            connectionDefinition.getName(), connectionDefinition.getTitle(), connectionDefinition.getVersion());
    }

    private TriggerDefinitionBasicDTO toTriggerDefinitionBasicDTO(TriggerDefinition triggerDefinition) {
        return new TriggerDefinitionBasicDTO(
            OptionalUtils.orElse(triggerDefinition.getBatch(), false), triggerDefinition.getDescription(),
            OptionalUtils.orElse(triggerDefinition.getHelp(), null), triggerDefinition.getName(),
            triggerDefinition.getTitle(), triggerDefinition.getType());
    }
}
