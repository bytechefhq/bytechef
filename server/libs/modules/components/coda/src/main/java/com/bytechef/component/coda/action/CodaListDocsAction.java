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

package com.bytechef.component.coda.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.coda.property.CodaDocListProperties;
import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class CodaListDocsAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("listDocs")
        .title("List Docs")
        .description("Returns a list of docs accessible by the user, and which they have opened at least once.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/docs"

            ))
        .properties(bool("isOwner").label("Is Owner")
            .description("Show only docs owned by the user.")
            .required(false)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            bool("isPublished").label("Is Published")
                .description("Show only published docs.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("limit").minValue(1)
                .label("Limit")
                .description("Maximum number of results to return in this query.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .output(outputSchema(object().properties(CodaDocListProperties.PROPERTIES)
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private CodaListDocsAction() {
    }
}
