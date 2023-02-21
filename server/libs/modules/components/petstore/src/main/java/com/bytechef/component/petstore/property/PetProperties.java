
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

package com.bytechef.component.petstore.property;

import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.definition.Property;
import java.util.List;

/**
 * Provides schema definition.
 *
 * @generated
 */
public class PetProperties {
    public static final List<Property> PROPERTIES = List.of(integer("id").label("Id")
        .required(false)
        .exampleValue(10),
        string("name").label("Name")
            .required(true)
            .exampleValue("doggie"),
        object("category").properties(CategoryProperties.PROPERTIES)
            .label("Category")
            .required(false),
        array("photoUrls").items(string(null))
            .placeholder("Add")
            .label("PhotoUrls")
            .required(true),
        array("tags").items(object(null).properties(TagProperties.PROPERTIES))
            .placeholder("Add")
            .label("Tags")
            .required(false),
        string("status").label("Status")
            .description("pet status in the store")
            .options(option("Available", "available"), option("Pending", "pending"), option("Sold", "sold"))
            .required(false));
}
