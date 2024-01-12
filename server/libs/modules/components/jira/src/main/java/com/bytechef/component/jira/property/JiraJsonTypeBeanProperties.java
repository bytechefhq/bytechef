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

import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class JiraJsonTypeBeanProperties {
    public static final List<ComponentDSL.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        string("type").label("Type")
            .description("The data type of the field.")
            .required(true),
        string("items").label("Items")
            .description("When the data type is an array, the name of the field items within the array.")
            .required(false),
        string("system").label("System")
            .description("If the field is a system field, the name of the field.")
            .required(false),
        string("custom").label("Custom")
            .description("If the field is a custom field, the URI of the field.")
            .required(false),
        integer("customId").label("Custom Id")
            .description("If the field is a custom field, the custom ID of the field.")
            .required(false),
        object("configuration").additionalProperties(object())
            .placeholder("Add to Configuration")
            .label("Configuration")
            .description("If the field is a custom field, the configuration of the field.")
            .required(false));
}
