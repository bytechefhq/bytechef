
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
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.definition.Property;
import java.util.List;

/**
 * Provides schema definition.
 *
 * @generated
 */
public class JiraIssueBeanProperties {
    public static final List<Property.ValueProperty<?>> PROPERTIES = List.of(string("expand").label("Expand")
        .description("Expand options that include additional issue details in the response.")
        .required(false),
        string("id").label("Id")
            .description("The ID of the issue.")
            .required(false),
        string("self").label("Self")
            .description("The URL of the issue details.")
            .required(false),
        string("key").label("Key")
            .description("The key of the issue.")
            .required(false),
        object("renderedFields").additionalProperties(object())
            .placeholder("Add to Rendered Fields")
            .label("Rendered Fields")
            .description("The rendered value of each field present on the issue.")
            .required(false),
        object("properties").additionalProperties(object())
            .placeholder("Add to Properties")
            .label("Properties")
            .description("Details of the issue properties identified in the request.")
            .required(false),
        object("names").additionalProperties(string())
            .placeholder("Add to Names")
            .label("Names")
            .description("The ID and name of each field present on the issue.")
            .required(false),
        object("schema").additionalProperties(object().properties(JiraJsonTypeBeanProperties.PROPERTIES))
            .placeholder("Add to Schema")
            .label("Schema")
            .description("The schema describing each field present on the issue.")
            .required(false),
        array("transitions").items(object().properties(JiraIssueTransitionProperties.PROPERTIES)
            .description("Details of an issue transition."))
            .placeholder("Add to Transitions")
            .label("Transitions")
            .description("The transitions that can be performed on the issue.")
            .required(false),
        object("operations").properties(JiraOperationsProperties.PROPERTIES)
            .label("Operations")
            .description("Details of the operations that can be performed on the issue.")
            .required(false),
        object("editmeta").properties(JiraIssueUpdateMetadataProperties.PROPERTIES)
            .label("Editmeta")
            .description("A list of editable field details.")
            .required(false),
        object("changelog").properties(JiraPageOfChangelogsProperties.PROPERTIES)
            .label("Changelog")
            .description("A page of changelogs.")
            .required(false),
        object("versionedRepresentations").additionalProperties(object())
            .placeholder("Add to Versioned Representations")
            .label("Versioned Representations")
            .description("The versions of each field on the issue.")
            .required(false),
        object("fieldsToInclude").properties(JiraIncludedFieldsProperties.PROPERTIES)
            .label("Fields To Include")
            .required(false),
        object("fields").additionalProperties(object())
            .placeholder("Add to Fields")
            .label("Fields")
            .required(false));
}
