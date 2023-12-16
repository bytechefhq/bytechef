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
public class JiraChangelogProperties {
    public static final List<ComponentDSL.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(string("id").label("Id")
        .description("The ID of the changelog.")
        .required(false),
        object("author").properties(JiraUserDetailsProperties.PROPERTIES)
            .placeholder("Add to Author")
            .label("Author")
            .description(
                "User details permitted by the user's Atlassian Account privacy settings. However, be aware of these exceptions:\n"
                    + "\n"
                    + " *  User record deleted from Atlassian: This occurs as the result of a right to be forgotten request. In this case, `displayName` provides an indication and other parameters have default values or are blank (for example, email is blank).\n"
                    + " *  User record corrupted: This occurs as a results of events such as a server import and can only happen to deleted users. In this case, `accountId` returns *unknown* and all other parameters have fallback values.\n"
                    + " *  User record unavailable: This usually occurs due to an internal service outage. In this case, all parameters have fallback values.")
            .required(false),
        dateTime("created").label("Created")
            .description("The date on which the change took place.")
            .required(false),
        array("items").items(object().properties(JiraChangeDetailsProperties.PROPERTIES)
            .description("A change item."))
            .placeholder("Add to Items")
            .label("Items")
            .description("The list of items changed.")
            .required(false),
        object("historyMetadata").properties(JiraHistoryMetadataProperties.PROPERTIES)
            .additionalProperties(
                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
            .placeholder("Add to History Metadata")
            .label("History Metadata")
            .description("Details of issue history metadata.")
            .required(false));
}
