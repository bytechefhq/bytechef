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
public class JiraIssueUpdateDetailsProperties {
    public static final List<ComponentDSL.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        object("transition").properties(JiraIssueTransitionProperties.PROPERTIES)
            .additionalProperties(
                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
            .placeholder("Add to Transition")
            .label("Transition")
            .description("Details of an issue transition.")
            .required(false),
        object("fields").additionalProperties(object())
            .placeholder("Add to Fields")
            .label("Fields")
            .description(
                "List of issue screen fields to update, specifying the sub-field to update and its value for each field. This field provides a straightforward option when setting a sub-field. When multiple sub-fields or other operations are required, use `update`. Fields included in here cannot be included in `update`.")
            .required(false),
        object("update").additionalProperties(array())
            .placeholder("Add to Update")
            .label("Update")
            .description(
                "A Map containing the field field name and a list of operations to perform on the issue screen field. Note that fields included in here cannot be included in `fields`.")
            .required(false),
        object("historyMetadata").properties(JiraHistoryMetadataProperties.PROPERTIES)
            .additionalProperties(
                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
            .placeholder("Add to History Metadata")
            .label("History Metadata")
            .description("Details of issue history metadata.")
            .required(false),
        array("properties").items(object().properties(JiraEntityPropertyProperties.PROPERTIES)
            .description(
                "An entity property, for more information see [Entity properties](https://developer.atlassian.com/cloud/jira/platform/jira-entity-properties/)."))
            .placeholder("Add to Properties")
            .label("Properties")
            .description("Details of issue properties to be add or update.")
            .required(false));
}
