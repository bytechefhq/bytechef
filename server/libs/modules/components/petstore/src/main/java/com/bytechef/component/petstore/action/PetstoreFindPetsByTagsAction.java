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
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.petstore.property.PetstorePetProperties;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PetstoreFindPetsByTagsAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("findPetsByTags")
        .title("Finds Pets by tags")
        .description("Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/pet/findByTags"

            ))
        .properties(array("tags").items(string())
            .placeholder("Add to Tags")
            .label("Tags")
            .description("Tags to filter by")
            .required(false)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)))
        .outputSchema(array().items(object().properties(PetstorePetProperties.PROPERTIES))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private PetstoreFindPetsByTagsAction() {
    }
}
