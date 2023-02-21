
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

package com.bytechef.component.petstore.action;

import static com.bytechef.hermes.component.RestComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.utils.HttpClientUtils.BodyContentType;
import static com.bytechef.hermes.component.utils.HttpClientUtils.ResponseFormat;

import com.bytechef.component.petstore.property.PetProperties;
import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class AddPetAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("addPet")
        .display(
            display("Add a new pet to the store")
                .description("Add a new pet to the store"))
        .metadata(
            Map.of(
                "requestMethod", "POST",
                "path", "/pet", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("pet").properties(PetProperties.PROPERTIES)
            .label("Pet")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .output(object(null).properties(PetProperties.PROPERTIES)
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)));
}
