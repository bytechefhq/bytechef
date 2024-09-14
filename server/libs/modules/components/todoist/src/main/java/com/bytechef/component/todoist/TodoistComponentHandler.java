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

package com.bytechef.component.todoist;

import static com.bytechef.component.definition.ComponentDSL.option;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.todoist.util.TodoistUtils;
import com.bytechef.definition.BaseProperty;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class TodoistComponentHandler extends AbstractTodoistComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/todoist.svg")
            .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "taskId")) {
            ((ModifiableStringProperty) modifiableProperty)
                .options(TodoistUtils.getOptions("/tasks", "content"));
        } else if (Objects.equals(modifiableProperty.getName(), "__item")) {
            Optional<List<? extends ValueProperty<?>>> propertiesOptional =
                ((ModifiableObjectProperty) modifiableProperty).getProperties();

            for (BaseProperty baseProperty : propertiesOptional.get()) {
                if (Objects.equals(baseProperty.getName(), "project_id")) {
                    ((ModifiableStringProperty) baseProperty)
                        .options(TodoistUtils.getOptions("/projects", "name"));
                } else if (Objects.equals(baseProperty.getName(), "color")) {
                    ((ModifiableStringProperty) baseProperty)
                        .options(
                            option("Berry red", "beryy_red"),
                            option("Red", "red"),
                            option("Orange", "orange"),
                            option("Yellow", "yellow"),
                            option("Olive green", "olive_green"),
                            option("Lime green", "lime_green"),
                            option("Green", "green"),
                            option("Mint green", "mint_green"),
                            option("Teal", "teal"),
                            option("Sky blue", "sky_blue"),
                            option("Light blue", "light_blue"),
                            option("Blue", "blue"),
                            option("Grape", "grape"),
                            option("Violet", "violet"),
                            option("Lavender", "lavender"),
                            option("Magenta", "magenta"),
                            option("Salmon", "salmon"),
                            option("Charcoal", "charcoal"),
                            option("Grey", "grey"),
                            option("Taupe", "taupe"));
                }
            }
        }

        return modifiableProperty;
    }
}
