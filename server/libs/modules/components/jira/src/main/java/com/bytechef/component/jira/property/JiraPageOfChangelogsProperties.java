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

package com.bytechef.component.jira.property;

import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class JiraPageOfChangelogsProperties {
    public static final List<ComponentDSL.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        integer("startAt").label("Start At")
            .description("The index of the first item returned on the page.")
            .required(false),
        integer("maxResults").label("Max Results")
            .description("The maximum number of results that could be on the page.")
            .required(false),
        integer("total").label("Total")
            .description("The number of results on the page.")
            .required(false),
        array("histories").items(object().properties(JiraChangelogProperties.PROPERTIES)
            .description("A changelog."))
            .placeholder("Add to Histories")
            .label("Histories")
            .description("The list of changelogs.")
            .required(false));
}
