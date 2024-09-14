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
public class PetstoreDeletePetAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("deletePet")
        .title("Deletes a pet")
        .description("delete a pet")
        .metadata(
            Map.of(
                "method", "DELETE",
                "path", "/pet/{petId}"

            ))
        .properties(string("api_key").label("Api Key")
            .description("")
            .required(false)
            .metadata(
                Map.of(
                    "type", PropertyType.HEADER)),
            integer("petId").label("Pet Id")
                .description("Pet id to delete")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)));

    private PetstoreDeletePetAction() {
    }
}
