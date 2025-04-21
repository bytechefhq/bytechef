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

package com.bytechef.component.petstore.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.petstore.property.PetstoreCategoryProperties;
import com.bytechef.component.petstore.property.PetstorePetProperties;
import com.bytechef.component.petstore.property.PetstoreTagProperties;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PetstoreAddPetAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("addPet")
        .title("Add a new pet to the store")
        .description("Add a new pet to the store")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/pet", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(integer("id").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Id")
            .required(false)
            .exampleValue(10),
            string("name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Name")
                .required(true)
                .exampleValue("doggie"),
            object("category").properties(PetstoreCategoryProperties.PROPERTIES)
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Category")
                .required(false),
            array("photoUrls").items(string().metadata(
                Map.of(
                    "type", PropertyType.BODY)))
                .placeholder("Add to Photo Urls")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Photo Urls")
                .required(true),
            array("tags").items(object().properties(PetstoreTagProperties.PROPERTIES))
                .placeholder("Add to Tags")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Tags")
                .required(false),
            string("status").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Status")
                .description("pet status in the store")
                .options(option("Available", "available"), option("Pending", "pending"), option("Sold", "sold"))
                .required(false))
        .output(outputSchema(object().properties(PetstorePetProperties.PROPERTIES)
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private PetstoreAddPetAction() {
    }
}
