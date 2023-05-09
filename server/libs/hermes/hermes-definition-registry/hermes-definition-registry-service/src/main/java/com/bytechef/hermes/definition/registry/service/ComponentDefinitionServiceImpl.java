
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
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistry;
import com.bytechef.hermes.definition.registry.dto.ActionDefinitionBasicDTO;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionBasicDTO;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionBasicDTO;
import com.bytechef.hermes.definition.registry.component.action.CustomAction;
import com.bytechef.hermes.definition.registry.util.DefinitionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionServiceImpl implements ComponentDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;

    @SuppressFBWarnings("EI2")
    public ComponentDefinitionServiceImpl(ComponentDefinitionRegistry componentDefinitionRegistry) {
        this.componentDefinitionRegistry = componentDefinitionRegistry;
    }

    @Override
    public ComponentDefinitionDTO getComponentDefinition(String name, Integer version) {
        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(name, version);

        return toComponentDefinitionDTO(componentDefinition);
    }

    @Override
    public List<ComponentDefinitionDTO> getComponentDefinitions() {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .map(this::toComponentDefinitionDTO)
            .toList();
    }

    @Override
    public List<ComponentDefinitionDTO> getComponentDefinitions(String name) {
        return componentDefinitionRegistry.getComponentDefinitions(name)
            .stream()
            .map(this::toComponentDefinitionDTO)
            .toList();
    }

    private ComponentDefinitionDTO toComponentDefinitionDTO(ComponentDefinition componentDefinition) {
        return new ComponentDefinitionDTO(
            getActions(componentDefinition),
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

    private List<ActionDefinitionBasicDTO> getActions(ComponentDefinition componentDefinition) {
        List<ActionDefinitionBasicDTO> actionDefinitionBasicDTOs = OptionalUtils.mapOrElse(
            componentDefinition.getActions(),
            actionDefinitions -> CollectionUtils.map(actionDefinitions, this::toActionDefinitionBasicDTO),
            Collections.emptyList());

        if (OptionalUtils.orElse(componentDefinition.getCustomAction(), false)) {
            actionDefinitionBasicDTOs = new ArrayList<>(actionDefinitionBasicDTOs);

            actionDefinitionBasicDTOs.add(
                toActionDefinitionBasicDTO(CustomAction.getCustomActionDefinition(componentDefinition)));
        }

        return actionDefinitionBasicDTOs;
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
