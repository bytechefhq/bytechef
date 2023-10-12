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

package com.bytechef.component.petstore.action;

import static com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.Context.Http.ResponseType;

import com.bytechef.component.petstore.property.PetstorePetProperties;
import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PetstoreGetPetByIdAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("getPetById")
        .title("Find pet by ID")
        .description("Returns a single pet")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/pet/{petId}"

            ))
        .properties(integer("petId").label("Pet Id")
            .description("ID of pet to return")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .outputSchema(object().properties(PetstorePetProperties.PROPERTIES)
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));
}
