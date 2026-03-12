/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.asana;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableNumberProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.definition.BaseProperty;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Monika Domiter
 */
@AutoService(OpenApiComponentHandler.class)
public class AsanaComponentHandler extends AbstractAsanaComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/asana.svg")
            .categories(ComponentCategory.PROJECT_MANAGEMENT)
            .customActionHelp(
                "Asana Web API documentation", "https://developers.asana.com/docs/api-explorer")
            .version(1);
    }

    @Override
    public List<ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {
        for (ModifiableActionDefinition actionDefinition : actionDefinitions) {
            String name = actionDefinition.getName();

            switch (name) {
                case "createCustomField" ->
                    actionDefinition.help(
                        "",
                        "https://docs.bytechef.io/reference/components/asana_v1#create-custom-field");
                case "createProject" ->
                    actionDefinition.help(
                        "",
                        "https://docs.bytechef.io/reference/components/asana_v1#create-project");
                case "createSubtask" ->
                    actionDefinition.help(
                        "",
                        "https://docs.bytechef.io/reference/components/asana_v1#create-subtask");
                case "createTask" ->
                    actionDefinition.help(
                        "",
                        "https://docs.bytechef.io/reference/components/asana_v1#create-task");
                default -> {
                }
            }
        }

        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .help("", "https://docs.bytechef.io/reference/components/asana_v1#connection-setup")
            .version(1);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (actionDefinition.getName()
            .equals("createCustomField")
            && Objects.equals(modifiableProperty.getName(), "data")) {

            Optional<List<? extends ValueProperty<?>>> propertiesOptional =
                ((ModifiableObjectProperty) modifiableProperty).getProperties();

            for (BaseProperty baseProperty : propertiesOptional.get()) {

                if (Objects.equals(baseProperty.getName(), "text_value")) {
                    ((ModifiableStringProperty) baseProperty)
                        .displayCondition("%s == '%s'".formatted("data.resource_subtype", "text"));
                }

                else if (Objects.equals(baseProperty.getName(), "enum_options")) {
                    ((ModifiableArrayProperty) baseProperty)
                        .displayCondition("%s == '%s' || %s == '%s'"
                            .formatted("data.resource_subtype", "enum",
                                "data.resource_subtype", "multi_enum"));
                }

                else if (Objects.equals(baseProperty.getName(), "number_value")) {
                    ((ModifiableNumberProperty) baseProperty)
                        .displayCondition("%s == '%s'".formatted("data.resource_subtype", "number"));
                }

                else if (Objects.equals(baseProperty.getName(), "precision")) {
                    ((ModifiableIntegerProperty) baseProperty)
                        .displayCondition("%s == '%s'".formatted("data.resource_subtype", "number"));
                }

                else if (Objects.equals(baseProperty.getName(), "date_value")) {
                    ((ModifiableObjectProperty) baseProperty)
                        .displayCondition("%s == '%s'".formatted("data.resource_subtype", "date"));
                }

                else if (Objects.equals(baseProperty.getName(), "people_value")) {
                    ((ModifiableArrayProperty) baseProperty)
                        .displayCondition("%s == '%s'".formatted("data.resource_subtype", "people"));
                }

                else if (Objects.equals(baseProperty.getName(), "reference_value")) {
                    ((ModifiableArrayProperty) baseProperty)
                        .displayCondition("%s == '%s'".formatted("data.resource_subtype", "reference"));
                }
            }
        }

        return modifiableProperty;
    }
}
