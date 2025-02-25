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

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.google.auto.service.AutoService;
import java.util.Objects;

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

        if (Objects.equals(modifiableProperty.getName(), "color")) {
            ((ModifiableStringProperty) modifiableProperty)
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

        return modifiableProperty;
    }
}
