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

package com.bytechef.component.nasa;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableDateProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableProperty;
import com.bytechef.component.definition.Property;
import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivona Pavela
 */
@AutoService(OpenApiComponentHandler.class)
public class NasaComponentHandler extends AbstractNasaComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .customActionHelp(
                "NASA Web API documentation", "https://api.nasa.gov/")
            .icon("path:assets/nasa.svg")
            .categories(ComponentCategory.DEVELOPER_TOOLS);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .help("", "https://docs.bytechef.io/reference/components/nasa_v1#connection-setup")
            .version(1);
    }

    @Override
    public List<ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {

        for (ModifiableActionDefinition modifiableActionDefinition : actionDefinitions) {
            if (Objects.equals(modifiableActionDefinition.getName(), "getPictureOfTheDay")) {
                Optional<List<? extends Property>> propertiesOptional = modifiableActionDefinition.getProperties();

                List<Property> properties = new ArrayList<>(propertiesOptional.orElse(List.of()));

                properties.addFirst(
                    string("queryType")
                        .label("Fetch Method")
                        .description("Select how you want to fetch the Picture of the Day.")
                        .options(
                            option("Single Date", "single"),
                            option("Date Range", "range"),
                            option("Random", "random"))
                        .required(true));

                modifiableActionDefinition.properties(properties);
            }
        }

        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        String name = modifiableProperty.getName();

        if (Objects.equals(name, "date")) {
            ((ModifiableDateProperty) modifiableProperty)
                .displayCondition("queryType == 'single'");
        }

        if (Objects.equals(name, "start_date")) {
            ((ModifiableDateProperty) modifiableProperty)
                .displayCondition("queryType == 'range'");
        }

        if (Objects.equals(name, "end_date")) {
            ((ModifiableDateProperty) modifiableProperty)
                .displayCondition("queryType == 'range'");
        }

        if (Objects.equals(name, "count")) {
            ((ModifiableIntegerProperty) modifiableProperty)
                .displayCondition("queryType == 'random'");
        }

        return modifiableProperty;
    }
}
