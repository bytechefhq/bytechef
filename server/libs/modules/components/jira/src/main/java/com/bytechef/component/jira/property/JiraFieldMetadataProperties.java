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
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class JiraFieldMetadataProperties {
    public static final List<ComponentDSL.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        bool("required").label("Required")
            .description("Whether the field is required.")
            .required(true),
        object("schema").properties(JiraJsonTypeBeanProperties.PROPERTIES)
            .placeholder("Add to Schema")
            .label("Schema")
            .description("The schema of a field.")
            .required(false),
        string("name").label("Name")
            .description("The name of the field.")
            .required(true),
        string("key").label("Key")
            .description("The key of the field.")
            .required(true),
        string("autoCompleteUrl").label("Auto Complete Url")
            .description("The URL that can be used to automatically complete the field.")
            .required(false),
        bool("hasDefaultValue").label("Has Default Value")
            .description("Whether the field has a default value.")
            .required(false),
        array("operations").items(string().description("The list of operations that can be performed on the field."))
            .placeholder("Add to Operations")
            .label("Operations")
            .description("The list of operations that can be performed on the field.")
            .required(true),
        array("allowedValues").items(object().description("The list of values allowed in the field."))
            .placeholder("Add to Allowed Values")
            .label("Allowed Values")
            .description("The list of values allowed in the field.")
            .required(false),
        object("defaultValue").label("Default Value")
            .description("The default value of the field.")
            .required(false),
        object("configuration").additionalProperties(object())
            .placeholder("Add to Configuration")
            .label("Configuration")
            .description("The configuration properties.")
            .required(false));
}
