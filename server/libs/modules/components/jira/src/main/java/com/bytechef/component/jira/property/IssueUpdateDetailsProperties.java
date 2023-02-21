
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
import static com.bytechef.hermes.component.definition.ComponentDSL.object;

import com.bytechef.hermes.definition.Property;
import java.util.List;

/**
 * Provides schema definition.
 *
 * @generated
 */
public class IssueUpdateDetailsProperties {
    public static final List<Property> PROPERTIES = List.of(
        object("transition").properties(IssueTransitionProperties.PROPERTIES)
            .label("Transition")
            .description("Details of an issue transition.")
            .required(false),
        object("fields").additionalProperties(object())
            .placeholder("Add")
            .label("Fields")
            .description(
                "List of issue screen fields to update, specifying the sub-field to update and its value for each field. This field provides a straightforward option when setting a sub-field. When multiple sub-fields or other operations are required, use `update`. Fields included in here cannot be included in `update`.")
            .required(false),
        object("update").additionalProperties(array())
            .placeholder("Add")
            .label("Update")
            .description(
                "A Map containing the field field name and a list of operations to perform on the issue screen field. Note that fields included in here cannot be included in `fields`.")
            .required(false),
        object("historyMetadata").properties(HistoryMetadataProperties.PROPERTIES)
            .label("HistoryMetadata")
            .description("Details of issue history metadata.")
            .required(false),
        array("properties").items(object(null).properties(EntityPropertyProperties.PROPERTIES)
            .description(
                "An entity property, for more information see [Entity properties](https://developer.atlassian.com/cloud/jira/platform/jira-entity-properties/)."))
            .placeholder("Add")
            .label("Properties")
            .description("Details of issue properties to be add or update.")
            .required(false));
}
