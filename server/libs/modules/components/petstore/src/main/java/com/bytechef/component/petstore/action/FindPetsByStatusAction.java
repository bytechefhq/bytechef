
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

import static com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;

import com.bytechef.component.petstore.property.PetProperties;
import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class FindPetsByStatusAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("findPetsByStatus")
        .display(
            display("Finds Pets by status")
                .description("Multiple status values can be provided with comma separated strings"))
        .metadata(
            Map.of(
                "requestMethod", "GET",
                "path", "/pet/findByStatus"

            ))
        .properties(string("status").label("Status")
            .description("Status values that need to be considered for filter")
            .options(option("Available", "available"), option("Pending", "pending"), option("Sold", "sold"))
            .required(false)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)))
        .output(array("array").items(object(null).properties(PetProperties.PROPERTIES))
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)));
}
