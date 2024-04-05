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

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PetstoreUpdatePetWithFormAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("updatePetWithForm")
        .title("Updates a pet in the store with form data")
        .description("")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/pet/{petId}"

            ))
        .properties(integer("petId").label("Pet Id")
            .description("ID of pet that needs to be updated")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("name").label("Name")
                .description("Name of pet that needs to be updated")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("status").label("Status")
                .description("Status of pet that needs to be updated")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)));

    private PetstoreUpdatePetWithFormAction() {
    }
}
