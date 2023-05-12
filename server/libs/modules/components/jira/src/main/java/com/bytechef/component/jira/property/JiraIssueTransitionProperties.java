
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

import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.definition.Property;
import java.util.List;

/**
 * Provides schema definition.
 *
 * @generated
 */
public class JiraIssueTransitionProperties {
    public static final List<Property> PROPERTIES = List.of(string("id").label("Id")
        .description("The ID of the issue transition. Required when specifying a transition to undertake.")
        .required(false),
        string("name").label("Name")
            .description("The name of the issue transition.")
            .required(false),
        object("to").properties(JiraStatusDetailsProperties.PROPERTIES)
            .label("To")
            .description("A status.")
            .required(false),
        bool("hasScreen").label("Has Screen")
            .description("Whether there is a screen associated with the issue transition.")
            .required(false),
        bool("isGlobal").label("Is Global")
            .description(
                "Whether the issue transition is global, that is, the transition is applied to issues regardless of their status.")
            .required(false),
        bool("isInitial").label("Is Initial")
            .description("Whether this is the initial issue transition for the workflow.")
            .required(false),
        bool("isAvailable").label("Is Available")
            .description("Whether the transition is available to be performed.")
            .required(false),
        bool("isConditional").label("Is Conditional")
            .description("Whether the issue has to meet criteria before the issue transition is applied.")
            .required(false),
        object("fields").additionalProperties(object().properties(JiraFieldMetadataProperties.PROPERTIES))
            .placeholder("Add")
            .label("Fields")
            .description(
                "Details of the fields associated with the issue transition screen. Use this information to populate `fields` and `update` in a transition request.")
            .required(false),
        string("expand").label("Expand")
            .description("Expand options that include additional transition details in the response.")
            .required(false),
        bool("looped").label("Looped")
            .required(false));
}
