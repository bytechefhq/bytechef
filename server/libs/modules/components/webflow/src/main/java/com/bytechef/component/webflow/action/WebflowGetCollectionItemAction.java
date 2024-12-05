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

package com.bytechef.component.webflow.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class WebflowGetCollectionItemAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getCollectionItem")
        .title("Get Collection Item")
        .description("Get collection item in a collection.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/v2/collections/{collectionId}/item/{itemId}"

            ))
        .properties(string("collectionId").label("Collection")
            .description("")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("itemId").label("Item")
                .description("")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)))
        .output(outputSchema(object()
            .properties(object("body")
                .properties(string("id").required(false),
                    object("fieldData").properties(string("name").required(false), string("slug").required(false))
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private WebflowGetCollectionItemAction() {
    }
}
