
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

import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.definition.Property;
import java.util.List;

/**
 * Provides schema definition.
 *
 * @generated
 */
public class JiraHistoryMetadataProperties {
    public static final List<Property.ValueProperty<?>> PROPERTIES = List.of(string("type").label("Type")
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
            .label("Actor")
            .description("Details of user or system associated with a issue history metadata item.")
            .required(false),
        object("generator").properties(JiraHistoryMetadataParticipantProperties.PROPERTIES)
            .label("Generator")
            .description("Details of user or system associated with a issue history metadata item.")
            .required(false),
        object("cause").properties(JiraHistoryMetadataParticipantProperties.PROPERTIES)
            .label("Cause")
            .description("Details of user or system associated with a issue history metadata item.")
            .required(false),
        object("extraData").additionalProperties(string())
            .placeholder("Add to Extra Data")
            .label("Extra Data")
            .description("Additional arbitrary information about the history record.")
            .required(false));
}
