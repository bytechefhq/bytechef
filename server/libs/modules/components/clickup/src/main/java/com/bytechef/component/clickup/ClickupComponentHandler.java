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

package com.bytechef.component.clickup;

import static com.bytechef.component.clickup.constant.ClickupConstants.FOLDER_ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.FOLDER_ID_PROPERTY;
import static com.bytechef.component.clickup.constant.ClickupConstants.SPACE_ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.SPACE_ID_PROPERTY;
import static com.bytechef.component.clickup.constant.ClickupConstants.WORKSPACE_ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.WORKSPACE_ID_PROPERTY;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.clickup.trigger.ClickupNewListTrigger;
import com.bytechef.component.clickup.trigger.ClickupNewTaskTrigger;
import com.bytechef.component.clickup.util.ClickupUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableNumberProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Property;
import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class ClickupComponentHandler extends AbstractClickupComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(
            ClickupNewListTrigger.TRIGGER_DEFINITION,
            ClickupNewTaskTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public List<? extends ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {
        for (ModifiableActionDefinition modifiableActionDefinition : actionDefinitions) {
            Optional<List<? extends Property>> propertiesOptional = modifiableActionDefinition.getProperties();
            List<Property> properties = new ArrayList<>(propertiesOptional.orElse(Collections.emptyList()));

            if (Objects.equals(modifiableActionDefinition.getName(), "createList")) {
                properties.addAll(0, List.of(WORKSPACE_ID_PROPERTY, SPACE_ID_PROPERTY));
            } else if (Objects.equals(modifiableActionDefinition.getName(), "createTask")) {
                properties.addAll(0, List.of(WORKSPACE_ID_PROPERTY, SPACE_ID_PROPERTY, FOLDER_ID_PROPERTY));
            } else if (Objects.equals(modifiableActionDefinition.getName(), "createFolder")) {
                properties.addFirst(WORKSPACE_ID_PROPERTY);
            }

            modifiableActionDefinition.properties(properties);
        }

        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/clickup.svg")
            .categories(ComponentCategory.PROJECT_MANAGEMENT);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "listId")) {
            ((ModifiableNumberProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) ClickupUtils::getAllListIdOptions)
                .optionsLookupDependsOn(FOLDER_ID, SPACE_ID, WORKSPACE_ID);
        } else if (Objects.equals(modifiableProperty.getName(), FOLDER_ID)) {
            ((ModifiableNumberProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) ClickupUtils::getFolderIdOptions)
                .optionsLookupDependsOn(SPACE_ID, WORKSPACE_ID);
        } else if (Objects.equals(modifiableProperty.getName(), "spaceId")) {
            ((ModifiableNumberProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) ClickupUtils::getSpaceIdOptions)
                .optionsLookupDependsOn(WORKSPACE_ID);
        }

        return modifiableProperty;
    }
}
