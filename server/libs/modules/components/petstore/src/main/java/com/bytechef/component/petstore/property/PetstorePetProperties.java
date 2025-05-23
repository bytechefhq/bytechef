/*
 * Copyright 2025 ByteChef
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

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class PetstorePetProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(integer("id").label("Id")
        .required(false)
        .exampleValue(10),
        string("name").label("Name")
            .required(true)
            .exampleValue("doggie"),
        object("category").properties(PetstoreCategoryProperties.PROPERTIES)
            .label("Category")
            .required(false),
        array("photoUrls").items(string())
            .placeholder("Add to Photo Urls")
            .label("Photo Urls")
            .required(true),
        array("tags").items(object().properties(PetstoreTagProperties.PROPERTIES))
            .placeholder("Add to Tags")
            .label("Tags")
            .required(false),
        string("status").label("Status")
            .description("pet status in the store")
            .options(option("Available", "available"), option("Pending", "pending"), option("Sold", "sold"))
            .required(false));

    private PetstorePetProperties() {
    }
}
