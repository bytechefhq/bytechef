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

package com.bytechef.component.tavily;

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
 * @author Marija Horvat
 */
@AutoService(OpenApiComponentHandler.class)
public class TavilyComponentHandler extends AbstractTavilyComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .icon("path:assets/tavily.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .customAction(true);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "topic")) {
            ((ModifiableStringProperty) modifiableProperty)
                .options(
                    option("General", "general", "General-purpose searches that may include a wide range of sources."),
                    option("News", "news", "Real-time updates."));
        } else if (Objects.equals(modifiableProperty.getName(), "search_depth")) {
            ((ModifiableStringProperty) modifiableProperty)
                .options(
                    option("Basic", "basic", "Provides generic content snippets from each source."),
                    option("Advanced", "advanced", "Retrieves the most relevant sources."));
        } else if (Objects.equals(modifiableProperty.getName(), "time_range")) {
            ((ModifiableStringProperty) modifiableProperty)
                .options(
                    option("Day", "day", "Results from the past 24 hours"),
                    option("Week", "week", "Results from the past 7 days"),
                    option("Month", "month", "Results from the past 30 days"),
                    option("Year", "year", "Results from the past 365 days"));
        } else if (Objects.equals(modifiableProperty.getName(), "extract_depth")) {
            ((ModifiableStringProperty) modifiableProperty)
                .options(
                    option("Basic", "basic", "Retrieves basic data."),
                    option("Advanced", "advanced", "Retrieves more data, including tables and embedded content."));
        }

        return modifiableProperty;
    }
}
