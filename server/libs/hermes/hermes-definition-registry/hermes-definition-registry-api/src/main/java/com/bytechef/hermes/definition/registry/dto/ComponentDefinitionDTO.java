
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

package com.bytechef.hermes.definition.registry.dto;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.definition.registry.component.action.CustomAction;
import com.bytechef.commons.util.IconUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ComponentDefinitionDTO(
    List<ActionDefinitionBasicDTO> actions, Optional<String> category,
    Optional<ConnectionDefinitionBasicDTO> connection, Optional<String> description, Optional<String> icon,
    String name, Optional<ResourcesDTO> resources, List<String> tags, List<TriggerDefinitionBasicDTO> triggers,
    String title, int version) {

    public ComponentDefinitionDTO(String name) {
        this(List.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), name, Optional.empty(),
            List.of(), List.of(), null, 1);
    }

    public ComponentDefinitionDTO(ComponentDefinition componentDefinition) {
        this(
            getActions(componentDefinition), componentDefinition.getCategory(), getConnection(componentDefinition),
            componentDefinition.getDescription(), getIcon(componentDefinition),
            componentDefinition.getName(), getResources(componentDefinition),
            OptionalUtils.orElse(componentDefinition.getTags(), null),
            OptionalUtils.mapOrElse(
                componentDefinition.getTriggers(),
                triggerDefinitions -> CollectionUtils.map(
                    triggerDefinitions, ComponentDefinitionDTO::toTriggerDefinitionBasicDTO),
                Collections.emptyList()),
            getTitle(componentDefinition.getName(), OptionalUtils.orElse(componentDefinition.getTitle(), null)),
            componentDefinition.getVersion());
    }

    private static List<ActionDefinitionBasicDTO> getActions(ComponentDefinition componentDefinition) {
        List<ActionDefinitionBasicDTO> actionDefinitionBasicDTOs = OptionalUtils.mapOrElse(
            componentDefinition.getActions(),
            actionDefinitions -> CollectionUtils.map(
                actionDefinitions, ComponentDefinitionDTO::toActionDefinitionBasicDTO),
            Collections.emptyList());

        if (OptionalUtils.orElse(componentDefinition.getCustomAction(), false)) {
            actionDefinitionBasicDTOs = new ArrayList<>(actionDefinitionBasicDTOs);

            actionDefinitionBasicDTOs.add(
                toActionDefinitionBasicDTO(CustomAction.getCustomActionDefinition(componentDefinition)));
        }

        return actionDefinitionBasicDTOs;
    }

    private static Optional<ConnectionDefinitionBasicDTO> getConnection(ComponentDefinition componentDefinition) {
        return componentDefinition.getConnection()
            .map(connectionDefinition -> toConnectionDefinitionDTO(connectionDefinition, componentDefinition));
    }

    private static Optional<String> getIcon(ComponentDefinition componentDefinition) {
        return componentDefinition.getIcon()
            .map(IconUtils::readIcon);
    }

    private static Optional<ResourcesDTO> getResources(ComponentDefinition componentDefinition) {
        return componentDefinition.getResources()
            .map(ResourcesDTO::new);
    }

    public static String getTitle(String componentName, String componentTitle) {
        return componentTitle == null ? componentName : componentTitle;
    }

    private static ActionDefinitionBasicDTO toActionDefinitionBasicDTO(
        ActionDefinition actionDefinition) {

        return new ActionDefinitionBasicDTO(actionDefinition);
    }

    private static ConnectionDefinitionBasicDTO toConnectionDefinitionDTO(
        ConnectionDefinition connectionDefinition, ComponentDefinition componentDefinition) {

        return new ConnectionDefinitionBasicDTO(connectionDefinition, componentDefinition);
    }

    private static TriggerDefinitionBasicDTO toTriggerDefinitionBasicDTO(
        TriggerDefinition triggerDefinition) {

        return new TriggerDefinitionBasicDTO(triggerDefinition);
    }
}
