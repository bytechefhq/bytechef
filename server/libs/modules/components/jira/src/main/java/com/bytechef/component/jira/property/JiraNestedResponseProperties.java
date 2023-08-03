
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

package com.bytechef.component.jira.property;

import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.date;
import static com.bytechef.hermes.component.definition.ComponentDSL.dateTime;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.nullable;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.ComponentDSL.time;

import com.bytechef.hermes.definition.DefinitionDSL;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class JiraNestedResponseProperties {
    public static final List<DefinitionDSL.ModifiableProperty.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        integer("status").label("Status")
            .required(false),
        object("errorCollection").properties(JiraErrorCollectionProperties.PROPERTIES)
            .additionalProperties(
                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
            .placeholder("Add to Error Collection")
            .label("Error Collection")
            .description("Error messages from an operation.")
            .required(false));
}
