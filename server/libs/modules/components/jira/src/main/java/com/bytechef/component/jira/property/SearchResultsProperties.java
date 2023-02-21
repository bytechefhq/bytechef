
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
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.definition.Property;
import java.util.List;

/**
 * Provides schema definition.
 *
 * @generated
 */
public class SearchResultsProperties {
    public static final List<Property> PROPERTIES = List.of(string("expand").label("Expand")
        .description("Expand options that include additional search result details in the response.")
        .required(false),
        integer("startAt").label("StartAt")
            .description("The index of the first item returned on the page.")
            .required(false),
        integer("maxResults").label("MaxResults")
            .description("The maximum number of results that could be on the page.")
            .required(false),
        integer("total").label("Total")
            .description("The number of results on the page.")
            .required(false),
        array("issues").items(object(null).properties(IssueBeanProperties.PROPERTIES)
            .description("Details about an issue."))
            .placeholder("Add")
            .label("Issues")
            .description("The list of issues found by the search.")
            .required(false),
        array("warningMessages").items(string(null).description("Any warnings related to the JQL query."))
            .placeholder("Add")
            .label("WarningMessages")
            .description("Any warnings related to the JQL query.")
            .required(false),
        object("names").additionalProperties(string())
            .placeholder("Add")
            .label("Names")
            .description("The ID and name of each field in the search results.")
            .required(false),
        object("schema").additionalProperties(object().properties(JsonTypeBeanProperties.PROPERTIES))
            .placeholder("Add")
            .label("Schema")
            .description("The schema describing the field types in the search results.")
            .required(false));
}
