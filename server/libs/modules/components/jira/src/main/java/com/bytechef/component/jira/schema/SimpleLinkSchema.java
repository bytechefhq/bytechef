
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

import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.definition.Property;
import java.util.List;

/**
 * Provides schema definition.
 *
 * @generated
 */
public class SimpleLinkSchema {
    public static final List<Property> COMPONENT_SCHEMA = List.of(string("id").label("Id")
        .required(false),
        string("styleClass").label("StyleClass")
            .required(false),
        string("iconClass").label("IconClass")
            .required(false),
        string("label").label("Label")
            .required(false),
        string("title").label("Title")
            .required(false),
        string("href").label("Href")
            .required(false),
        integer("weight").label("Weight")
            .required(false));
}
