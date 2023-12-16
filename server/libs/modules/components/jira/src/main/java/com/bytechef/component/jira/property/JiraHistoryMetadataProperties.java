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
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.date;
import static com.bytechef.hermes.component.definition.ComponentDSL.dateTime;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.nullable;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.ComponentDSL.time;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class JiraHistoryMetadataProperties {
    public static final List<ComponentDSL.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        string("type").label("Type")
            .description("The type of the history record.")
            .required(false),
        string("description").label("Description")
            .description("The description of the history record.")
            .required(false),
        string("descriptionKey").label("Description Key")
            .description("The description key of the history record.")
            .required(false),
        string("activityDescription").label("Activity Description")
            .description("The activity described in the history record.")
            .required(false),
        string("activityDescriptionKey").label("Activity Description Key")
            .description("The key of the activity described in the history record.")
            .required(false),
        string("emailDescription").label("Email Description")
            .description("The description of the email address associated the history record.")
            .required(false),
        string("emailDescriptionKey").label("Email Description Key")
            .description("The description key of the email address associated the history record.")
            .required(false),
        object("actor").properties(JiraHistoryMetadataParticipantProperties.PROPERTIES)
            .additionalProperties(
                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
            .placeholder("Add to Actor")
            .label("Actor")
            .description("Details of user or system associated with a issue history metadata item.")
            .required(false),
        object("generator").properties(JiraHistoryMetadataParticipantProperties.PROPERTIES)
            .additionalProperties(
                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
            .placeholder("Add to Generator")
            .label("Generator")
            .description("Details of user or system associated with a issue history metadata item.")
            .required(false),
        object("cause").properties(JiraHistoryMetadataParticipantProperties.PROPERTIES)
            .additionalProperties(
                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
            .placeholder("Add to Cause")
            .label("Cause")
            .description("Details of user or system associated with a issue history metadata item.")
            .required(false),
        object("extraData").additionalProperties(string())
            .placeholder("Add to Extra Data")
            .label("Extra Data")
            .description("Additional arbitrary information about the history record.")
            .required(false));
}
