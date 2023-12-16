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
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class JiraLinkGroupProperties {
    public static final List<ComponentDSL.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(string("id").label("Id")
        .required(false),
        string("styleClass").label("Style Class")
            .required(false),
        object("header").properties(JiraSimpleLinkProperties.PROPERTIES)
            .placeholder("Add to Header")
            .label("Header")
            .description("Details about the operations available in this version.")
            .required(false),
        integer("weight").label("Weight")
            .required(false),
        array("links").items(object().properties(JiraSimpleLinkProperties.PROPERTIES)
            .description("Details about the operations available in this version."))
            .placeholder("Add to Links")
            .label("Links")
            .required(false));
}
