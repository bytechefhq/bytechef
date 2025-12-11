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

package com.bytechef.component.productboard.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.productboard.property.ProductboardExpandedNoteProperties;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ProductboardListNotesAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("listNotes")
        .title("List All Notes")
        .description("Returns detail of all notes order by created_at desc")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/notes"

            ))
        .properties()
        .output(outputSchema(object().properties(
            array("data").items(object().properties(ProductboardExpandedNoteProperties.PROPERTIES))
                .required(false),
            string("pageCursor").description(
                "Use this pageCursor returned by search results to get next page of results. GET /notes?pageCursor={pageCursor}")
                .required(false),
            integer("totalResults")
                .description("Total number of notes that meet the specified parameters for the query")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ProductboardListNotesAction() {
    }
}
