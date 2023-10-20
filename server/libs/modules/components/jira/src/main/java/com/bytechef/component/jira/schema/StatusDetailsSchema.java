
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

package com.bytechef.component.jira.schema;

import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.definition.Property;
import java.util.List;

/**
 * Provides schema definition.
 *
 * @generated
 */
public class StatusDetailsSchema {
    public static final List<Property> COMPONENT_SCHEMA = List.of(string("self").label("Self")
        .description("The URL of the status.")
        .required(false),
        string("description").label("Description")
            .description("The description of the status.")
            .required(false),
        string("iconUrl").label("IconUrl")
            .description("The URL of the icon used to represent the status.")
            .required(false),
        string("name").label("Name")
            .description("The name of the status.")
            .required(false),
        string("id").label("Id")
            .description("The ID of the status.")
            .required(false),
        object("statusCategory").properties(StatusCategorySchema.COMPONENT_SCHEMA)
            .label("StatusCategory")
            .description("A status category.")
            .required(false));
}
